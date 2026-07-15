package com.fooddelivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails (currently just the password-reset link) via
 * Resend's HTTPS API (https://resend.com/docs/api-reference/emails/send-email).
 *
 * Deliberately NOT using SMTP: most cloud hosts (Railway included, on its
 * Free/Trial/Hobby plans) block outbound SMTP entirely to fight spam abuse —
 * see https://docs.railway.com/networking/outbound-networking. A plain HTTPS
 * POST on port 443 sidesteps that completely, since it's indistinguishable
 * from any other API call the app makes.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${resend.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String fromAddress;

    /**
     * @return true if Resend accepted the email for delivery, false if sending
     *         failed (caller decides how to degrade gracefully — e.g. showing
     *         the link on-screen instead, which matters a lot before the API
     *         key is configured or before a sending domain is verified).
     */
    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("Cannot send password reset email — RESEND_API_KEY is not set. " +
                "Get a free key at https://resend.com/api-keys and set it as an env var.");
            return false;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
            "from",    "QuickBite <" + fromAddress + ">",
            "to",      List.of(toEmail),
            "subject", "Reset your QuickBite password",
            "html",    buildHtmlBody(userName, resetLink)
        );

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                RESEND_API_URL, new HttpEntity<>(body, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Password reset email sent to {} via Resend", toEmail);
                return true;
            }
            log.error("Resend returned {} sending to {}: {}",
                response.getStatusCode(), toEmail, response.getBody());
            return false;
        } catch (RestClientException e) {
            // Covers 4xx/5xx from Resend (e.g. "you can only send to your own
            // address until you verify a domain") as well as network failures.
            // Don't let a bad response break the forgot-password flow — log it
            // clearly so it's easy to diagnose, and let the caller fall back.
            log.error("Failed to send password reset email to {} via Resend: {}", toEmail, e.getMessage());
            return false;
        }
    }

    private String buildHtmlBody(String userName, String resetLink) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:480px;margin:0 auto;
                        background:#fff;border-radius:16px;overflow:hidden;border:1px solid #eee;">
              <div style="background:linear-gradient(135deg,#e53935,#ff7043);padding:28px 32px;">
                <h1 style="margin:0;color:#fff;font-size:1.4rem;">🍔 QuickBite</h1>
              </div>
              <div style="padding:28px 32px;">
                <p style="font-size:1rem;color:#333;">Hi %s,</p>
                <p style="font-size:.95rem;color:#555;line-height:1.6;">
                  We received a request to reset your QuickBite password. Click the button
                  below to choose a new one. This link expires in <b>1 hour</b>.
                </p>
                <div style="text-align:center;margin:28px 0;">
                  <a href="%s"
                     style="background:#e53935;color:#fff;text-decoration:none;font-weight:700;
                            padding:12px 28px;border-radius:10px;display:inline-block;font-size:.95rem;">
                    Reset Password
                  </a>
                </div>
                <p style="font-size:.82rem;color:#999;line-height:1.5;">
                  If you didn't request this, you can safely ignore this email — your
                  password won't be changed. If the button above doesn't work, copy and
                  paste this link into your browser:<br>
                  <a href="%s" style="color:#e53935;word-break:break-all;">%s</a>
                </p>
              </div>
            </div>
            """.formatted(userName, resetLink, resetLink, resetLink);
    }
}

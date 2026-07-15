package com.fooddelivery.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends transactional emails (currently just the password-reset link) via
 * SMTP, configured through spring.mail.* properties in application.properties.
 *
 * Kept deliberately simple — one method, one email — but structured so more
 * email types (order confirmations, etc.) can be added the same way later.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * @return true if the email was handed off to the SMTP server successfully,
     *         false if sending failed (caller decides how to degrade gracefully —
     *         e.g. showing the link on-screen instead, which is invaluable
     *         during local development before real SMTP creds are set up).
     */
    public boolean sendPasswordResetEmail(String toEmail, String userName, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, "QuickBite");
            helper.setTo(toEmail);
            helper.setSubject("Reset your QuickBite password");
            helper.setText(buildHtmlBody(userName, resetLink), true);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
            return true;
        } catch (MailException | java.io.UnsupportedEncodingException | jakarta.mail.MessagingException e) {
            // Don't let a broken SMTP config break the forgot-password flow —
            // log it clearly so it's easy to diagnose, and let the caller fall back.
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
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

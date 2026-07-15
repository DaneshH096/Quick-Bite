package com.fooddelivery.controller;

import com.fooddelivery.model.PasswordResetToken;
import com.fooddelivery.model.User;
import com.fooddelivery.service.EmailService;
import com.fooddelivery.service.PasswordResetService;
import com.fooddelivery.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * PasswordResetController — handles forgot-password and reset-password flows.
 *
 * URLs (all public — no login required):
 *   GET  /forgot-password            → show "enter your email" form
 *   POST /forgot-password            → create token, EMAIL the reset link
 *   GET  /reset-password?token=UUID  → show "enter new password" form
 *   POST /reset-password             → apply the new password
 */
@Controller
public class PasswordResetController {

    @Autowired private PasswordResetService passwordResetService;
    @Autowired private EmailService         emailService;
    @Autowired private UserService          userService;

    // Falls back to this if the request's own host/port can't be trusted
    // (e.g. behind a reverse proxy) — set APP_BASE_URL in application.properties.
    @Value("${app.base-url}")
    private String configuredBaseUrl;

    // ── Step 1: Show forgot-password form ─────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    // ── Step 2: Process email, generate token, EMAIL the reset link ───
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                         HttpServletRequest request,
                                         Model model) {
        // Always show the same "sent" message even if the email isn't found
        // (prevents attackers from using this form to enumerate real accounts)
        model.addAttribute("email", email);

        Optional<String> tokenOpt = passwordResetService.createResetToken(email);

        if (tokenOpt.isPresent()) {
            String token = tokenOpt.get();
            String resetLink = configuredBaseUrl + "/reset-password?token=" + token;

            User user = userService.findByEmail(email.trim().toLowerCase()).orElseThrow();
            boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetLink);

            model.addAttribute("tokenFound", true);
            model.addAttribute("emailSent", emailSent);
            if (!emailSent) {
                // SMTP isn't configured / failed — don't leave the person stuck.
                // Show the link directly with a clear warning instead of
                // silently pretending an email went out.
                model.addAttribute("resetLink", resetLink);
            }
        } else {
            // Email not found — still show the same success-like page (no enumeration)
            model.addAttribute("tokenFound", false);
        }

        return "auth/forgot-password-sent";
    }

    // ── Step 3: Show reset-password form (token from URL) ─────────
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> tokenOpt = passwordResetService.validateToken(token);

        if (tokenOpt.isEmpty()) {
            // Token invalid or expired — show clear error
            model.addAttribute("errorTitle",   "Link Expired or Invalid");
            model.addAttribute("errorMessage", "This password reset link has expired (links are valid for 1 hour) or has already been used. Please request a new one.");
            model.addAttribute("errorEmoji",   "⏰");
            model.addAttribute("backUrl",      "/forgot-password");
            return "error/general";
        }

        model.addAttribute("token", token);
        model.addAttribute("email", tokenOpt.get().getUser().getEmail());
        return "auth/reset-password";
    }

    // ── Step 4: Apply new password ────────────────────────────────
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                        @RequestParam String password,
                                        @RequestParam String confirmPassword,
                                        Model model) {
        // Client-side validation was already done; server-side is the guard
        if (!password.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "Passwords do not match.");
            passwordResetService.getEmailForToken(token)
                .ifPresent(e -> model.addAttribute("email", e));
            return "auth/reset-password";
        }

        try {
            passwordResetService.resetPassword(token, password);
            return "redirect:/login?passwordReset=true";
        } catch (IllegalArgumentException e) {
            // Token expired between form load and submit
            model.addAttribute("errorTitle",   "Reset Failed");
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("errorEmoji",   "⏰");
            model.addAttribute("backUrl",      "/forgot-password");
            return "error/general";
        }
    }
}

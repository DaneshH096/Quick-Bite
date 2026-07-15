package com.fooddelivery.service;

import com.fooddelivery.model.PasswordResetToken;
import com.fooddelivery.model.User;
import com.fooddelivery.repository.PasswordResetTokenRepository;
import com.fooddelivery.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final int TOKEN_VALID_MINUTES = 60; // 1 hour

    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private UserRepository               userRepository;
    @Autowired private PasswordEncoder              passwordEncoder;

    /**
     * Step 1 — User submits their email.
     *
     * Finds the user, deletes any existing tokens for them,
     * creates a fresh UUID token with 1-hour expiry, and saves it.
     *
     * Returns the raw token string so the controller can build the link and
     * hand it to EmailService to actually send it.
     *
     * Returns empty Optional if no user found with that email
     * (caller handles the "email not found" case transparently to avoid enumeration).
     */
    @Transactional
    public Optional<String> createResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return Optional.empty(); // Don't reveal whether email exists
        }
        User user = userOpt.get();

        // Revoke any existing tokens for this user
        tokenRepository.deleteByUserId(user.getId());

        // Create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALID_MINUTES))
            .used(false)
            .createdAt(LocalDateTime.now())
            .build();
        tokenRepository.save(resetToken);

        return Optional.of(token);
    }

    /**
     * Step 2 — Validate the token from the URL.
     * Returns the token entity if valid, empty if expired/used/not found.
     */
    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> validateToken(String token) {
        return tokenRepository.findByToken(token)
            .filter(PasswordResetToken::isValid);
    }

    /**
     * Step 3 — User submits new password.
     * Validates the token again, updates the password, marks token as used.
     *
     * @throws IllegalArgumentException if token is invalid/expired
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Reset link is invalid or has expired. Please request a new one."));

        if (!resetToken.isValid()) {
            throw new IllegalArgumentException("Reset link has expired or already been used. Please request a new one.");
        }

        // Enforce minimum password length
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long.");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate the token
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    /**
     * Get user email from token (for display in the reset form).
     */
    @Transactional(readOnly = true)
    public Optional<String> getEmailForToken(String token) {
        return tokenRepository.findByToken(token)
            .filter(PasswordResetToken::isValid)
            .map(t -> t.getUser().getEmail());
    }
}

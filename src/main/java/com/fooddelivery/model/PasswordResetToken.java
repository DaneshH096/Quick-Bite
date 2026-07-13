package com.fooddelivery.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * PasswordResetToken — stores a one-time reset token per user.
 *
 * Lifecycle:
 *   1. User submits /forgot-password with their email
 *   2. A token (UUID) is saved here with expiry = now + 1 hour
 *   3. User receives a link: /reset-password?token=<uuid>
 *   4. They submit the new password — token is validated (exists + not expired + not used)
 *   5. Token is marked used=true, password is updated
 *
 * In this app the "link" is shown on-screen (no email server needed).
 * In production: send via email using Spring Mail + SMTP.
 */
@Entity
@Table(name = "password_reset_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID token — used in the reset link */
    @Column(nullable = false, unique = true)
    private String token;

    /** Which user this token belongs to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** When this token expires (default: 1 hour from creation) */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Prevents replay attacks — token can only be used once */
    @Column(name = "used")
    private boolean used = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Check if the token is still valid */
    public boolean isValid() {
        return !used && LocalDateTime.now().isBefore(expiresAt);
    }
}

package com.upc.pre.peaceapp.security.iam.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Token temporal de recuperación de contraseña. Solo se guarda el hash SHA-256:
 * el valor en claro únicamente existe en el canal de envío al usuario.
 */
@Entity
@Table(name = "iam_password_reset_tokens")
public class PasswordResetToken {
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(nullable = false)
    private Long userId;

    @Getter
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Getter
    @Column(nullable = false)
    private Instant expiresAt;

    protected PasswordResetToken() {}

    public PasswordResetToken(Long userId, String rawToken, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = hash(rawToken);
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public static String hash(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

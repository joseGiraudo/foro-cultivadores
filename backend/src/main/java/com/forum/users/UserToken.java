package com.forum.users;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Token de un solo uso (verificación de email o reset de contraseña). Se persiste el HASH del token,
 * nunca el valor en claro (ese solo viaja en el email). Tiene expiración y se marca al usarse.
 */
@Entity
@Table(name = "user_tokens")
class UserToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TokenPurpose purpose;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    protected UserToken() {
        // requerido por JPA
    }

    UserToken(Long userId, String tokenHash, TokenPurpose purpose, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    Long getUserId() {
        return userId;
    }

    TokenPurpose getPurpose() {
        return purpose;
    }

    /** Usable = no usado y no vencido. */
    boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    void markUsed() {
        this.usedAt = Instant.now();
    }
}

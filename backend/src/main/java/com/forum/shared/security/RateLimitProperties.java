package com.forum.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config del rate-limiting de autenticación, bajo {@code forum.rate-limit}. Controla cuántos POST
 * por IP se aceptan en una ventana fija sobre los endpoints sensibles de {@code /auth} antes de
 * responder 429. Ver {@link RateLimitFilter}.
 */
@ConfigurationProperties(prefix = "forum.rate-limit")
public class RateLimitProperties {

    /** Interruptor general. Default true; los tests lo apagan para no toparse con el límite. */
    private boolean enabled = true;

    /** Máximo de intentos por IP y por endpoint dentro de la ventana. */
    private int maxAttempts = 10;

    /** Duración de la ventana, en segundos. */
    private long windowSeconds = 60;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
}

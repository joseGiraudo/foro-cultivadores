package com.forum.shared.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate-limiting in-memory por IP sobre los endpoints sensibles de autenticación (login, registro,
 * olvido/reset de contraseña, reenvío de verificación). Frena fuerza bruta de contraseñas y abuso
 * que dispara emails (que además queman la cuota de Resend). Ventana fija configurable en
 * {@code forum.rate-limit}: hasta {@code max-attempts} por IP y por endpoint cada {@code window-seconds};
 * al superarla responde 429 con {@code Retry-After} (mismo formato ProblemDetail que el resto).
 *
 * <p>Corre antes de Spring Security ({@link Ordered#HIGHEST_PRECEDENCE}) y deja pasar todo lo que no
 * sea un POST a una ruta protegida. In-memory alcanza para una sola instancia (igual que la sesión);
 * para escalar horizontalmente haría falta un store compartido (Redis). Se apaga con
 * {@code forum.rate-limit.enabled=false} (los tests lo apagan para repetir logins/registros sin tope).
 */
@Component
@ConditionalOnProperty(prefix = "forum.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
class RateLimitFilter extends OncePerRequestFilter implements Ordered {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/auth/login", "/auth/register", "/auth/forgot-password",
            "/auth/reset-password", "/auth/resend-verification");
    /** Tope de claves rastreadas; si se supera se purgan las ventanas vencidas (evita crecer sin fin). */
    private static final int MAX_TRACKED = 50_000;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !PROTECTED_PATHS.contains(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (windows.size() > MAX_TRACKED) {
            windows.values().removeIf(w -> now - w.start >= properties.getWindowSeconds());
        }

        String key = clientIp(request) + ' ' + request.getRequestURI();
        Window window = windows.compute(key, (k, current) -> {
            if (current == null || now - current.start >= properties.getWindowSeconds()) {
                return new Window(now);
            }
            current.count++;
            return current;
        });

        if (window.count > properties.getMaxAttempts()) {
            long retryAfter = Math.max(1, properties.getWindowSeconds() - (now - window.start));
            tooManyRequests(response, retryAfter);
            return;
        }
        chain.doFilter(request, response);
    }

    /** IP del cliente; respeta X-Forwarded-For (primer salto) por si hay un reverse proxy delante. */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void tooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos. Probá de nuevo en un rato.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        objectMapper.writeValue(response.getWriter(), problem);
    }

    /** Ventana fija: instante de inicio (epoch s) y cantidad de requests contadas. */
    private static final class Window {
        private final long start;
        private int count;

        private Window(long start) {
            this.start = start;
            this.count = 1;
        }
    }
}

package com.forum.users;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.email.EmailProperties;
import com.forum.email.EmailSender;
import com.forum.shared.error.ApiException;

/**
 * Flujos de cuenta que dependen del email: verificación de email y recuperación de contraseña.
 * Emite tokens de un solo uso (se guarda solo el hash), con expiración, y envía los links por email.
 * No depende de {@link UserService} (usa el repositorio directo) para evitar ciclos.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private static final Duration VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration RESET_TTL = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final UserTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final EmailProperties emailProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    AccountService(UserRepository userRepository, UserTokenRepository tokenRepository,
                   PasswordEncoder passwordEncoder, EmailSender emailSender, EmailProperties emailProperties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.emailProperties = emailProperties;
    }

    /** Tras registrar: emite un token de verificación y manda el email. */
    @Transactional
    public void sendVerificationEmail(User user) {
        String raw = issueToken(user.getId(), TokenPurpose.EMAIL_VERIFICATION, VERIFICATION_TTL);
        String link = link("/verify-email", raw);
        safeSend(user.getEmail(), "Verificá tu cuenta",
                "<p>¡Bienvenido/a al Foro de Cultivadores!</p>"
                        + "<p>Confirmá tu email para activar tu cuenta:</p>"
                        + "<p><a href=\"" + link + "\">Verificar mi email</a></p>"
                        + "<p>El enlace vence en 24 horas.</p>");
    }

    /** Reenvía la verificación si el email existe y aún no está verificado (respuesta genérica). */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(email)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    /** Verifica el email a partir del token del link. 400 si el enlace es inválido o venció. */
    @Transactional
    public void verifyEmail(String rawToken) {
        UserToken token = consume(rawToken, TokenPurpose.EMAIL_VERIFICATION);
        userRepository.findById(token.getUserId()).ifPresent(User::markEmailVerified);
    }

    /** Si el email existe, emite token de reset y manda el link (respuesta genérica igual). */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String raw = issueToken(user.getId(), TokenPurpose.PASSWORD_RESET, RESET_TTL);
            String link = link("/reset-password", raw);
            safeSend(user.getEmail(), "Recuperá tu contraseña",
                    "<p>Recibimos un pedido para restablecer tu contraseña.</p>"
                            + "<p><a href=\"" + link + "\">Elegir una nueva contraseña</a></p>"
                            + "<p>El enlace vence en 1 hora. Si no fuiste vos, ignorá este email.</p>");
        });
    }

    /** Cambia la contraseña a partir del token de reset. Verifica el email de paso (probó la casilla). */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        UserToken token = consume(rawToken, TokenPurpose.PASSWORD_RESET);
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.badRequest("El enlace es inválido o expiró"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.markEmailVerified();
    }

    // --- helpers ---

    private String issueToken(Long userId, TokenPurpose purpose, Duration ttl) {
        tokenRepository.deleteByUserIdAndPurpose(userId, purpose); // invalida tokens previos del mismo tipo
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new UserToken(userId, sha256Hex(raw), purpose, Instant.now().plus(ttl)));
        return raw;
    }

    private UserToken consume(String rawToken, TokenPurpose purpose) {
        UserToken token = tokenRepository.findByTokenHash(sha256Hex(rawToken))
                .filter(t -> t.getPurpose() == purpose && t.isUsable(Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("El enlace es inválido o expiró"));
        token.markUsed();
        return token;
    }

    private String link(String path, String rawToken) {
        return emailProperties.getFrontendBaseUrl() + path + "?token=" + rawToken;
    }

    private void safeSend(String to, String subject, String html) {
        // Best-effort: un fallo de email no debe romper el registro ni el pedido del usuario.
        try {
            emailSender.send(to, subject, html);
        } catch (RuntimeException e) {
            log.error("No se pudo enviar el email a {}", to, e);
        }
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

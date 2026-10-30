package com.forum.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.forum.bootstrap.BootstrapProperties;

/**
 * Crea el primer ADMIN en producción cuando {@code forum.bootstrap.enabled=true} y todavía no existe
 * ningún ADMIN. Sin esto la base de prod arrancaría sin administradores (el {@link UserSeeder} de
 * ejemplo es solo perfil {@code dev}) y nadie podría crear categorías ni promover usuarios. Lee las
 * credenciales de {@code forum.bootstrap.admin-*}; el usuario queda con email verificado y términos
 * aceptados para poder iniciar sesión de entrada. Idempotente: si ya hay un ADMIN, no hace nada.
 */
@Component
@ConditionalOnProperty(prefix = "forum.bootstrap", name = "enabled", havingValue = "true")
@Order(1)
class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    /** Espejo del mínimo del registro (RegisterRequest valida password ≥ 8). */
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapProperties properties;

    AdminBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
                   BootstrapProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            log.info("Bootstrap: ya existe al menos un ADMIN, no se crea otro.");
            return;
        }

        String username = trimToNull(properties.getAdminUsername());
        String email = trimToNull(properties.getAdminEmail());
        String password = properties.getAdminPassword();

        if (username == null || email == null || password == null || password.isBlank()) {
            log.warn("Bootstrap activado pero faltan credenciales del admin "
                    + "(forum.bootstrap.admin-username/-email/-password); no se crea ADMIN.");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.warn("Bootstrap: la contraseña del admin es muy corta (mínimo {} caracteres); "
                    + "no se crea ADMIN.", MIN_PASSWORD_LENGTH);
            return;
        }
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            log.warn("Bootstrap: el username o email del admin ya están en uso; no se crea ADMIN.");
            return;
        }

        User admin = new User(username, email, passwordEncoder.encode(password),
                Role.ADMIN, UserStatus.ACTIVE);
        admin.markEmailVerified();
        admin.acceptTerms(UserService.TERMS_VERSION);
        userRepository.save(admin);
        log.info("Bootstrap: creado el primer ADMIN «{}». Cambiá la contraseña tras el primer login.",
                username);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

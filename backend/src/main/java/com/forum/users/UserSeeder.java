package com.forum.users;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Carga usuarios de prueba solo con el perfil {@code dev} y solo si la tabla está vacía.
 * El resto del contenido de ejemplo (posts, comentarios, follows) se completa en el hito de follows.
 */
@Component
@Profile("dev")
@Order(1)
class UserSeeder implements ApplicationRunner {

    static final String DEFAULT_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    UserSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }

        create("admin", "admin@forum.test", Role.ADMIN, UserStatus.ACTIVE, null);
        create("mod", "mod@forum.test", Role.MODERATOR, UserStatus.ACTIVE, null);
        create("grow_norte", "grow_norte@forum.test", Role.CONTRIBUTOR, UserStatus.ACTIVE,
                "Growshop del norte. Genéticas, sustratos y asesoramiento para tu cultivo.");
        create("grow_sur", "grow_sur@forum.test", Role.CONTRIBUTOR, UserStatus.ACTIVE,
                "Growshop del sur. Equipamiento indoor y guías para principiantes.");
        create("user1", "user1@forum.test", Role.USER, UserStatus.ACTIVE, null);
        create("user2", "user2@forum.test", Role.USER, UserStatus.ACTIVE, null);
        create("user3", "user3@forum.test", Role.USER, UserStatus.ACTIVE, null);
        create("suspended", "suspended@forum.test", Role.USER, UserStatus.SUSPENDED, null);
    }

    private void create(String username, String email, Role role, UserStatus status, String bio) {
        User user = new User(username, email, passwordEncoder.encode(DEFAULT_PASSWORD), role, status);
        user.setBio(bio);
        userRepository.save(user);
    }
}

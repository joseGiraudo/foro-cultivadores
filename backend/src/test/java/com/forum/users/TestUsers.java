package com.forum.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Helper de tests para crear usuarios de cualquier rol/estado desde cualquier módulo.
 * Vive en el paquete {@code com.forum.users} para poder usar el repositorio package-private,
 * y es público para que otros paquetes de test lo inyecten.
 */
@Component
public class TestUsers {

    public static final String PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    TestUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(String username, Role role, UserStatus status) {
        return userRepository.save(new User(username, username + "@forum.test",
                passwordEncoder.encode(PASSWORD), role, status));
    }
}

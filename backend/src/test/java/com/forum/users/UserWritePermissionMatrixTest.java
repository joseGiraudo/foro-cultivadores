package com.forum.users;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.forum.IntegrationTestSupport;

/**
 * Matriz de permisos de escritura para las acciones del hito de usuarios y auth: para cada actor
 * (anónimo, cada rol, y suspendido) se verifica el código HTTP esperado. Es la red de seguridad
 * ante cambios de reglas.
 */
class UserWritePermissionMatrixTest extends IntegrationTestSupport {

    private static final String PASSWORD = "password123";
    private static final String ANONYMOUS = "";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User target;

    @BeforeEach
    void setUp() {
        save("m_user", Role.USER, UserStatus.ACTIVE);
        save("m_contributor", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        save("m_moderator", Role.MODERATOR, UserStatus.ACTIVE);
        save("m_admin", Role.ADMIN, UserStatus.ACTIVE);
        save("m_suspended", Role.USER, UserStatus.SUSPENDED);
        target = save("m_target", Role.USER, UserStatus.ACTIVE);
    }

    private User save(String username, Role role, UserStatus status) {
        return userRepository.save(new User(username, username + "@forum.test",
                passwordEncoder.encode(PASSWORD), role, status));
    }

    private void perform(MockHttpServletRequestBuilder request, String actor, int expected) throws Exception {
        if (!ANONYMOUS.equals(actor)) {
            request = request.with(httpBasic(actor, PASSWORD));
        }
        mvc.perform(request).andExpect(status().is(expected));
    }

    // PATCH /users/me: cualquier usuario activo puede; suspendido 403; anónimo 401.
    static java.util.stream.Stream<Arguments> updateMeActors() {
        return java.util.stream.Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("m_user", 200),
                Arguments.of("m_contributor", 200),
                Arguments.of("m_moderator", 200),
                Arguments.of("m_admin", 200),
                Arguments.of("m_suspended", 403));
    }

    @ParameterizedTest(name = "PATCH /users/me actor={0} -> {1}")
    @MethodSource("updateMeActors")
    void updateMe(String actor, int expected) throws Exception {
        perform(patch("/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("bio", "x"))), actor, expected);
    }

    // Endpoints de admin: solo ADMIN activo; suspendido/otros roles 403; anónimo 401.
    static java.util.stream.Stream<Arguments> adminActors() {
        return java.util.stream.Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("m_user", 403),
                Arguments.of("m_contributor", 403),
                Arguments.of("m_moderator", 403),
                Arguments.of("m_admin", 200),
                Arguments.of("m_suspended", 403));
    }

    @ParameterizedTest(name = "PATCH admin role actor={0} -> {1}")
    @MethodSource("adminActors")
    void adminChangeRole(String actor, int expected) throws Exception {
        perform(patch("/admin/users/" + target.getId() + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("role", "CONTRIBUTOR"))), actor, expected);
    }

    @ParameterizedTest(name = "PATCH admin status actor={0} -> {1}")
    @MethodSource("adminActors")
    void adminChangeStatus(String actor, int expected) throws Exception {
        perform(patch("/admin/users/" + target.getId() + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", "SUSPENDED"))), actor, expected);
    }
}

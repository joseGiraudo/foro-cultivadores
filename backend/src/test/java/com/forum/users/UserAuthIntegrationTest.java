package com.forum.users;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.forum.IntegrationTestSupport;

class UserAuthIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = "password123";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User admin;
    private User normalUser;
    private User suspendedUser;

    @BeforeEach
    void setUp() {
        admin = save("admin", "admin@forum.test", Role.ADMIN, UserStatus.ACTIVE);
        normalUser = save("user1", "user1@forum.test", Role.USER, UserStatus.ACTIVE);
        suspendedUser = save("suspended", "suspended@forum.test", Role.USER, UserStatus.SUSPENDED);
    }

    private User save(String username, String email, Role role, UserStatus status) {
        return userRepository.save(new User(username, email, passwordEncoder.encode(PASSWORD), role, status));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // --- Registro ---

    @Test
    void register_valid_returns201WithoutEmail() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "nuevo", "email", "nuevo@forum.test", "password", "secret123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("nuevo"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "user1", "email", "otro@forum.test", "password", "secret123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "otro", "email", "user1@forum.test", "password", "secret123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidFields_returns400WithFieldErrors() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("username", "a b", "email", "no-es-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    // --- /users/me ---

    @Test
    void me_withoutAuth_returns401() throws Exception {
        mvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_authenticated_returnsProfileWithEmail() throws Exception {
        mvc.perform(get("/users/me").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.email").value("user1@forum.test"));
    }

    @Test
    void me_wrongPassword_returns401() throws Exception {
        mvc.perform(get("/users/me").with(httpBasic("user1", "incorrecta")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateMe_updatesBio() throws Exception {
        mvc.perform(patch("/users/me").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bio", "Cultivo en balcón"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Cultivo en balcón"));
    }

    @Test
    void updateMe_suspended_returns403() throws Exception {
        mvc.perform(patch("/users/me").with(httpBasic("suspended", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("bio", "no debería poder"))))
                .andExpect(status().isForbidden());
    }

    // --- Perfil público ---

    @Test
    void publicProfile_hidesEmail() throws Exception {
        mvc.perform(get("/users/" + normalUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user1"))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void publicProfile_notFound_returns404() throws Exception {
        mvc.perform(get("/users/999999"))
                .andExpect(status().isNotFound());
    }

    // --- Admin ---

    @Test
    void adminChangeRole_promotesUser() throws Exception {
        mvc.perform(patch("/admin/users/" + normalUser.getId() + "/role").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "CONTRIBUTOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CONTRIBUTOR"));
    }

    @Test
    void adminChangeOwnRole_returns409() throws Exception {
        mvc.perform(patch("/admin/users/" + admin.getId() + "/role").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("role", "USER"))))
                .andExpect(status().isConflict());
    }

    @Test
    void adminChangeStatus_suspendsUser() throws Exception {
        mvc.perform(patch("/admin/users/" + normalUser.getId() + "/status").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void adminChangeOwnStatus_returns409() throws Exception {
        mvc.perform(patch("/admin/users/" + admin.getId() + "/status").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isConflict());
    }

    @Test
    void adminList_asAdmin_returnsPageWithEmail() throws Exception {
        mvc.perform(get("/admin/users").with(httpBasic("admin", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.content[0].email").exists());
    }

    @Test
    void adminList_filterByStatus() throws Exception {
        mvc.perform(get("/admin/users").param("status", "SUSPENDED").with(httpBasic("admin", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("suspended"));
    }

    @Test
    void adminList_asNonAdmin_returns403() throws Exception {
        mvc.perform(get("/admin/users").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminList_anonymous_returns401() throws Exception {
        mvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}

package com.forum.categories;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.UserStatus;

class CategoryIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        users.create("admin", Role.ADMIN, UserStatus.ACTIVE);
        users.create("user1", Role.USER, UserStatus.ACTIVE);
        users.create("admin_susp", Role.ADMIN, UserStatus.SUSPENDED);
        categoryRepository.save(new Category("Interior", "Cultivo indoor"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void list_isPublic_andSortedByName() throws Exception {
        categoryRepository.save(new Category("Exterior", null));
        mvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Exterior"))
                .andExpect(jsonPath("$[1].name").value("Interior"));
    }

    @Test
    void create_asAdmin_returns201() throws Exception {
        mvc.perform(post("/categories").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Genéticas", "description", "Variedades"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Genéticas"))
                .andExpect(jsonPath("$.description").value("Variedades"));
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        mvc.perform(post("/categories").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Interior"))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_blankName_returns400() throws Exception {
        mvc.perform(post("/categories").with(httpBasic("admin", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void create_asNonAdmin_returns403() throws Exception {
        mvc.perform(post("/categories").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nueva"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_anonymous_returns401() throws Exception {
        mvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nueva"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_asSuspendedAdmin_returns403() throws Exception {
        mvc.perform(post("/categories").with(httpBasic("admin_susp", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nueva"))))
                .andExpect(status().isForbidden());
    }
}

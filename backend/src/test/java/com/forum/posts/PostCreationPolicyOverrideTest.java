package com.forum.posts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.forum.TestcontainersConfiguration;
import com.forum.categories.Category;
import com.forum.categories.TestCategories;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.UserStatus;

import tools.jackson.databind.ObjectMapper;

/**
 * Demuestra que subir el min-role de creación de QUESTION a CONTRIBUTOR (solo por config) impide
 * que un USER haga preguntas, sin tocar código.
 */
@SpringBootTest(properties = "forum.rules.post-creation-min-role.QUESTION=CONTRIBUTOR")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestUsers.class, TestCategories.class})
@Transactional
class PostCreationPolicyOverrideTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestUsers users;

    @Autowired
    private TestCategories categories;

    private Category interior;

    @BeforeEach
    void setUp() {
        users.create("user1", Role.USER, UserStatus.ACTIVE);
        users.create("grow", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        interior = categories.create("Interior");
    }

    private String questionBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", "¿Consulta de prueba?", "content", "Contenido",
                "postType", "QUESTION", "categoryIds", List.of(interior.getId())));
    }

    @Test
    void user_cannotCreateQuestion_whenMinRoleRaised() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("user1", TestUsers.PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(questionBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void contributor_canCreateQuestion_whenMinRoleRaised() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("grow", TestUsers.PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(questionBody()))
                .andExpect(status().isCreated());
    }
}

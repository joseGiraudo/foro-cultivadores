package com.forum.posts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.categories.Category;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

/**
 * editedAt debe reflejar solo ediciones de contenido, no cambios de estado (moderación).
 * Esa es la razón de ser del campo, separado de updatedAt.
 */
class PostEditedFlagTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    private User author;
    private Category interior;

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        author = users.create("author", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        interior = categories.create("Interior");
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void newPost_editedAtIsNull() throws Exception {
        var p = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(get("/posts/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editedAt").doesNotExist());
    }

    @Test
    void afterEdit_editedAtIsSet() throws Exception {
        var p = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId()).with(httpBasic("author", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "contenido editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editedAt").exists());

        mvc.perform(get("/posts/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editedAt").exists());
    }

    @Test
    void afterStatusChange_editedAtStaysNull() throws Exception {
        var p = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        // Un cambio de estado (ocultar) mueve updatedAt pero NO debe marcar editedAt.
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk());

        mvc.perform(get("/posts/" + p.getId()).with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.editedAt").doesNotExist());
    }
}

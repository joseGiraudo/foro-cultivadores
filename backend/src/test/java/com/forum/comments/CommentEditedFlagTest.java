package com.forum.comments;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.categories.Category;
import com.forum.posts.Post;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

/**
 * editedAt en comentarios refleja solo ediciones de contenido, no cambios de estado (moderación).
 */
class CommentEditedFlagTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    @Autowired
    private CommentRepository commentRepository;

    private User user1;
    private Post question;

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        user1 = users.create("user1", Role.USER, UserStatus.ACTIVE);
        Category interior = categories.create("Interior");
        question = posts.create(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
    }

    private Comment saveComment(String content) {
        return commentRepository.save(new Comment(content, user1, question, null));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void newComment_editedAtIsNull() throws Exception {
        saveComment("original");
        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].editedAt").doesNotExist());
    }

    @Test
    void afterEdit_editedAtIsSet() throws Exception {
        Comment c = saveComment("original");
        mvc.perform(patch("/comments/" + c.getId()).with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.editedAt").exists());
    }

    @Test
    void afterStatusChange_editedAtStaysNull() throws Exception {
        Comment c = saveComment("original");
        // Ocultar (cambio de estado) no debe marcar editedAt.
        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk());

        mvc.perform(get("/posts/" + question.getId() + "/comments").with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$[0].editedAt").doesNotExist());
    }
}

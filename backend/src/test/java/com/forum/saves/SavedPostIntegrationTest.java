package com.forum.saves;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class SavedPostIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    private User author;
    private Category interior;
    private Post post;

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        users.create("user1", Role.USER, UserStatus.ACTIVE);
        users.create("user2", Role.USER, UserStatus.ACTIVE);
        users.create("suspended", Role.USER, UserStatus.SUSPENDED);
        author = users.create("author", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        interior = categories.create("Interior");
        post = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // --- Guardar ---

    @Test
    void save_thenAppearsInSavedList() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/saved-posts").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(post.getId()))
                .andExpect(jsonPath("$.content[0].saved").value(true));
    }

    @Test
    void save_anonymous_returns401() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void save_suspended_returns403() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("suspended", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void save_nonExistentPost_returns404() throws Exception {
        mvc.perform(post("/posts/999999/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void save_hiddenPost_asUser_returns404() throws Exception {
        Post hidden = posts.create(author, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(post("/posts/" + hidden.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void save_isIdempotent() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/saved-posts").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    // --- Quitar de guardados ---

    @Test
    void unsave_removesFromList() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/saved-posts").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void unsave_whenNotSaved_isIdempotent() throws Exception {
        mvc.perform(delete("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
    }

    // --- Flag saved en el detalle ---

    @Test
    void postDetail_savedFlag_reflectsPerUser() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/posts/" + post.getId()).with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
        mvc.perform(get("/posts/" + post.getId()).with(httpBasic("user2", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));
        mvc.perform(get("/posts/" + post.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(false));
    }

    // --- Listado ---

    @Test
    void savedList_ordersByMostRecentlySavedFirst() throws Exception {
        Post first = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        Post second = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);

        mvc.perform(post("/posts/" + first.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/posts/" + second.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/saved-posts").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[1].id").value(first.getId()));
    }

    @Test
    void savedList_excludesPostThatBecameHidden() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/posts/" + post.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk());

        mvc.perform(get("/me/saved-posts").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void savedList_isolatedPerUser() throws Exception {
        mvc.perform(post("/posts/" + post.getId() + "/save").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/me/saved-posts").with(httpBasic("user2", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void savedList_anonymous_returns401() throws Exception {
        mvc.perform(get("/me/saved-posts")).andExpect(status().isUnauthorized());
    }
}

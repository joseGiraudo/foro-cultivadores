package com.forum.posts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.categories.Category;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

class PostIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    @Autowired
    private PostRepository postRepository;

    private User user1;
    private User grow;
    private Category interior;

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        user1 = users.create("user1", Role.USER, UserStatus.ACTIVE);
        users.create("user2", Role.USER, UserStatus.ACTIVE);
        grow = users.create("grow", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        users.create("suspended", Role.USER, UserStatus.SUSPENDED);
        interior = categories.create("Interior");
    }

    private Post newPost(User author, PostType type, ContentStatus status, Category... cats) {
        Post post = new Post("Título de prueba", "Contenido de prueba", author, type,
                new LinkedHashSet<>(Set.of(cats)));
        if (status != ContentStatus.ACTIVE) {
            post.changeStatus(status);
        }
        return postRepository.save(post);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // --- Creación ---

    @Test
    void createQuestion_asUser_returns201() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "¿Hojas amarillas?", "content", "Ayuda con mi planta",
                                "postType", "QUESTION", "categoryIds", List.of(interior.getId())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postType").value("QUESTION"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.author.username").value("user1"))
                .andExpect(jsonPath("$.categories[0].name").value("Interior"));
    }

    @Test
    void createArticle_asUser_returns403_dueToMinRole() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Nueva genética", "content", "Reseña",
                                "postType", "ARTICLE", "categoryIds", List.of(interior.getId())))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createArticle_asContributor_returns201() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("grow", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Nueva genética", "content", "Reseña",
                                "postType", "ARTICLE", "categoryIds", List.of(interior.getId())))))
                .andExpect(status().isCreated());
    }

    @Test
    void create_anonymous_returns401() throws Exception {
        mvc.perform(post("/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Título válido", "content", "x",
                                "postType", "QUESTION", "categoryIds", List.of(interior.getId())))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_suspended_returns403() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("suspended", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Título válido", "content", "x",
                                "postType", "QUESTION", "categoryIds", List.of(interior.getId())))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_tooShortTitle_returns400() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "abc", "content", "x",
                                "postType", "QUESTION", "categoryIds", List.of(interior.getId())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void create_tooManyCategories_returns400() throws Exception {
        Category c2 = categories.create("Exterior");
        Category c3 = categories.create("Equipamiento");
        Category c4 = categories.create("Genéticas");
        mvc.perform(post("/posts").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Título válido", "content", "x", "postType", "QUESTION",
                                "categoryIds", List.of(interior.getId(), c2.getId(), c3.getId(), c4.getId())))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_nonExistentCategory_returns400() throws Exception {
        mvc.perform(post("/posts").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Título válido", "content", "x",
                                "postType", "QUESTION", "categoryIds", List.of(999999)))))
                .andExpect(status().isBadRequest());
    }

    // --- Lectura y visibilidad ---

    @Test
    void get_activePost_isPublic() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(get("/posts/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Contenido de prueba"))
                .andExpect(jsonPath("$.author.role").value("USER"));
    }

    @Test
    void get_nonExistent_returns404() throws Exception {
        mvc.perform(get("/posts/999999")).andExpect(status().isNotFound());
    }

    @Test
    void get_hiddenPost_asAnonymous_returns404() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(get("/posts/" + p.getId())).andExpect(status().isNotFound());
    }

    @Test
    void get_hiddenPost_asModerator_returns200() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(get("/posts/" + p.getId()).with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    // --- Listado ---

    @Test
    void list_anonymous_showsOnlyActive_withExcerptAndNoContent() throws Exception {
        newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        newPost(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].excerpt").value("Contenido de prueba"))
                .andExpect(jsonPath("$.content[0].content").doesNotExist());
    }

    @Test
    void list_filterByAuthor() throws Exception {
        newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        newPost(grow, PostType.ARTICLE, ContentStatus.ACTIVE, interior);
        mvc.perform(get("/posts").param("authorId", grow.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].author.username").value("grow"));
    }

    @Test
    void list_filterByPostType() throws Exception {
        newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        newPost(grow, PostType.ARTICLE, ContentStatus.ACTIVE, interior);
        mvc.perform(get("/posts").param("postType", "ARTICLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].postType").value("ARTICLE"));
    }

    @Test
    void list_moderatorCanFilterByStatus() throws Exception {
        newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        newPost(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(get("/posts").param("status", "HIDDEN").with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("HIDDEN"));
    }

    @Test
    void list_nonModStatusFilterIsIgnored() throws Exception {
        newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        newPost(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(get("/posts").param("status", "HIDDEN").with(httpBasic("user2", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"));
    }

    // --- Edición ---

    @Test
    void update_byAuthor_updatesTitle() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId()).with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Título editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Título editado"))
                .andExpect(jsonPath("$.content").value("Contenido de prueba"));
    }

    @Test
    void update_byNonAuthor_returns403() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId()).with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Intruso edita"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_hiddenPost_authorCannotSee_returns404() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(patch("/posts/" + p.getId()).with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "Título editado"))))
                .andExpect(status().isNotFound());
    }

    // --- Transiciones de estado ---

    @Test
    void status_authorDeletesOwnPost() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELETED_BY_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED_BY_USER"));
    }

    @Test
    void status_authorCannotHide_returns409() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void status_moderatorHidesAndRestores() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void status_terminalDeleted_cannotTransition_returns409() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.DELETED_BY_MOD, interior);
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isConflict());
    }

    @Test
    void status_byRandomUser_returns403() throws Exception {
        Post p = newPost(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        mvc.perform(patch("/posts/" + p.getId() + "/status").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELETED_BY_USER"))))
                .andExpect(status().isForbidden());
    }
}

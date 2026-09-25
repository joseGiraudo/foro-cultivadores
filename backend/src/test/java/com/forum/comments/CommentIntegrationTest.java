package com.forum.comments;

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

import com.forum.IntegrationTestSupport;
import com.forum.categories.Category;
import com.forum.posts.Post;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

class CommentIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    @Autowired
    private CommentRepository commentRepository;

    private User user1;
    private User user2;
    private Category interior;
    private Post question; // QUESTION (comment-order SCORE) por user1
    private Post article;  // ARTICLE (comment-order CHRONOLOGICAL) por grow

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        user1 = users.create("user1", Role.USER, UserStatus.ACTIVE);
        user2 = users.create("user2", Role.USER, UserStatus.ACTIVE);
        users.create("suspended", Role.USER, UserStatus.SUSPENDED);
        User grow = users.create("grow", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        interior = categories.create("Interior");
        question = posts.create(user1, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        article = posts.create(grow, PostType.ARTICLE, ContentStatus.ACTIVE, interior);
    }

    private Comment saveComment(Post post, User author, Comment parent, String content) {
        return commentRepository.save(new Comment(content, author, post, parent));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // --- Creación ---

    @Test
    void create_topLevel_returns201() throws Exception {
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "Probá bajar el riego"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Probá bajar el riego"))
                .andExpect(jsonPath("$.depth").value(0))
                .andExpect(jsonPath("$.parentCommentId").doesNotExist())
                .andExpect(jsonPath("$.author.username").value("user1"));
    }

    @Test
    void create_anonymous_returns401() throws Exception {
        mvc.perform(post("/posts/" + question.getId() + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "hola"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_suspended_returns403() throws Exception {
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("suspended", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "hola"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_onHiddenPost_asAnonymous_returns404() throws Exception {
        Post hidden = posts.create(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(post("/posts/" + hidden.getId() + "/comments").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "hola"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_onHiddenPost_asModerator_returns409() throws Exception {
        Post hidden = posts.create(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(post("/posts/" + hidden.getId() + "/comments").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "hola"))))
                .andExpect(status().isConflict());
    }

    @Test
    void reply_withValidParent_returns201() throws Exception {
        Comment parent = saveComment(question, user1, null, "raíz");
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "respuesta", "parentCommentId", parent.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentCommentId").value(parent.getId()));
    }

    @Test
    void reply_withParentFromAnotherPost_returns400() throws Exception {
        Comment otherPostComment = saveComment(article, user1, null, "en otro post");
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "respuesta", "parentCommentId", otherPostComment.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reply_withNonExistentParent_returns400() throws Exception {
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "respuesta", "parentCommentId", 999999))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reply_withNonActiveParent_returns400() throws Exception {
        Comment parent = saveComment(question, user1, null, "raíz");
        parent.changeStatus(ContentStatus.DELETED_BY_USER);
        commentRepository.save(parent);
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "respuesta", "parentCommentId", parent.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_blankContent_returns400() throws Exception {
        mvc.perform(post("/posts/" + question.getId() + "/comments").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.content").exists());
    }

    // --- Listado: recorrido en profundidad ---

    @Test
    void list_depthFirstTraversalWithDepths() throws Exception {
        Comment root1 = saveComment(article, user1, null, "root1");
        Comment reply1a = saveComment(article, user2, root1, "reply1a");
        saveComment(article, user1, reply1a, "reply1a1");
        saveComment(article, user2, root1, "reply1b");
        saveComment(article, user1, null, "root2");

        mvc.perform(get("/posts/" + article.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].content").value("root1")).andExpect(jsonPath("$[0].depth").value(0))
                .andExpect(jsonPath("$[1].content").value("reply1a")).andExpect(jsonPath("$[1].depth").value(1))
                .andExpect(jsonPath("$[2].content").value("reply1a1")).andExpect(jsonPath("$[2].depth").value(2))
                .andExpect(jsonPath("$[3].content").value("reply1b")).andExpect(jsonPath("$[3].depth").value(1))
                .andExpect(jsonPath("$[4].content").value("root2")).andExpect(jsonPath("$[4].depth").value(0));
    }

    @Test
    void list_hiddenComment_contentHiddenForAnonymous_statusVisible() throws Exception {
        Comment c = saveComment(question, user1, null, "contenido oculto");
        c.changeStatus(ContentStatus.HIDDEN);
        commentRepository.save(c);

        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$[0].content").doesNotExist());
    }

    @Test
    void list_hiddenComment_contentVisibleForModerator() throws Exception {
        Comment c = saveComment(question, user1, null, "contenido oculto");
        c.changeStatus(ContentStatus.HIDDEN);
        commentRepository.save(c);

        mvc.perform(get("/posts/" + question.getId() + "/comments").with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$[0].content").value("contenido oculto"));
    }

    @Test
    void list_hiddenPost_asAnonymous_returns404() throws Exception {
        Post hidden = posts.create(user1, PostType.QUESTION, ContentStatus.HIDDEN, interior);
        mvc.perform(get("/posts/" + hidden.getId() + "/comments")).andExpect(status().isNotFound());
    }

    // --- Edición ---

    @Test
    void update_byAuthor_returns200() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        mvc.perform(patch("/comments/" + c.getId()).with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "editado"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("editado"));
    }

    @Test
    void update_byNonAuthor_returns403() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        mvc.perform(patch("/comments/" + c.getId()).with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "intruso"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_nonActiveComment_returns409() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        c.changeStatus(ContentStatus.DELETED_BY_USER);
        commentRepository.save(c);
        mvc.perform(patch("/comments/" + c.getId()).with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("content", "editado"))))
                .andExpect(status().isConflict());
    }

    // --- Transiciones de estado ---

    @Test
    void status_authorDeletesOwnComment() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELETED_BY_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELETED_BY_USER"));
    }

    @Test
    void status_authorCannotHide_returns409() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("user1", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isConflict());
    }

    @Test
    void status_moderatorHides_returns200() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"));
    }

    @Test
    void status_byRandomUser_returns403() throws Exception {
        Comment c = saveComment(question, user1, null, "original");
        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELETED_BY_USER"))))
                .andExpect(status().isForbidden());
    }
}

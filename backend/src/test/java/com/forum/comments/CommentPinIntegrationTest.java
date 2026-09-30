package com.forum.comments;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.categories.Category;
import com.forum.posts.Post;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

class CommentPinIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    @Autowired
    private CommentRepository commentRepository;

    private User author; // dueño del post
    private User ua;
    private User ub;
    private User voter;
    private Category interior;
    private Post question; // QUESTION → orden por SCORE

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        author = users.create("author", Role.USER, UserStatus.ACTIVE);
        ua = users.create("ua", Role.USER, UserStatus.ACTIVE);
        ub = users.create("ub", Role.USER, UserStatus.ACTIVE);
        voter = users.create("voter", Role.USER, UserStatus.ACTIVE);
        interior = categories.create("Interior");
        question = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
    }

    private Comment saveComment(Post post, User commentAuthor, Comment parent, String content) {
        return commentRepository.save(new Comment(content, commentAuthor, post, parent));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    // --- Fijar ---

    @Test
    void pin_byPostAuthor_returns200_andPinnedFlag() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true));
    }

    @Test
    void pin_byRandomUser_returns403() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("ub", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pin_byModeratorNotAuthor_returns403() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pin_anonymous_returns401() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pin_reply_returns400() throws Exception {
        Comment root = saveComment(question, ua, null, "raíz");
        Comment reply = saveComment(question, ub, root, "respuesta");
        mvc.perform(post("/comments/" + reply.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pin_nonActiveComment_returns409() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        c.changeStatus(ContentStatus.HIDDEN);
        commentRepository.save(c);
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isConflict());
    }

    @Test
    void pin_nonExistentComment_returns404() throws Exception {
        mvc.perform(post("/comments/999999/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void pin_bySuspendedPostAuthor_returns403() throws Exception {
        User suspendedAuthor = users.create("susAuthor", Role.USER, UserStatus.SUSPENDED);
        Post ownPost = posts.create(suspendedAuthor, PostType.QUESTION, ContentStatus.ACTIVE, interior);
        Comment c = saveComment(ownPost, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("susAuthor", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void pin_replacesPreviousPin() throws Exception {
        Comment c1 = saveComment(question, ua, null, "c1");
        Comment c2 = saveComment(question, ub, null, "c2");
        mvc.perform(post("/comments/" + c1.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(post("/comments/" + c2.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());

        // Solo c2 queda fijado; c1 deja de estarlo (uno por post). c2 va primero.
        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("c2"))
                .andExpect(jsonPath("$[0].pinned").value(true))
                .andExpect(jsonPath("$[1].content").value("c1"))
                .andExpect(jsonPath("$[1].pinned").value(false));
    }

    // --- Orden: el fijado va primero aunque el score diga otra cosa ---

    @Test
    void pinnedComment_appearsFirst_overridingScoreOrder() throws Exception {
        Comment cHigh = saveComment(question, ua, null, "cHigh");
        Comment cPin = saveComment(question, ub, null, "cPin");
        votes.voteComment(voter.getId(), cHigh.getId(), VoteValue.UPVOTE); // cHigh = +1, cPin = 0

        // Sin fijar, el orden por SCORE sería cHigh, cPin.
        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(jsonPath("$[0].content").value("cHigh"))
                .andExpect(jsonPath("$[1].content").value("cPin"));

        mvc.perform(post("/comments/" + cPin.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());

        // Fijado, cPin pasa a estar primero.
        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("cPin"))
                .andExpect(jsonPath("$[0].pinned").value(true))
                .andExpect(jsonPath("$[1].content").value("cHigh"))
                .andExpect(jsonPath("$[1].pinned").value(false));
    }

    // --- Desfijar ---

    @Test
    void unpin_byPostAuthor_removesPin() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(delete("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pinned").value(false));
    }

    @Test
    void unpin_byNonAuthor_returns403() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());
        mvc.perform(delete("/comments/" + c.getId() + "/pin").with(httpBasic("ub", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    // --- Auto-desfijado al dejar de estar activo ---

    @Test
    void pinnedComment_autoUnpinned_whenHiddenByModerator() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());

        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));

        mvc.perform(get("/posts/" + question.getId() + "/comments").with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$[0].pinned").value(false));
    }

    @Test
    void pinnedComment_autoUnpinned_whenDeletedByItsAuthor() throws Exception {
        Comment c = saveComment(question, ua, null, "un comentario");
        mvc.perform(post("/comments/" + c.getId() + "/pin").with(httpBasic("author", PASSWORD)))
                .andExpect(status().isOk());

        mvc.perform(patch("/comments/" + c.getId() + "/status").with(httpBasic("ua", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "DELETED_BY_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(false));
    }
}

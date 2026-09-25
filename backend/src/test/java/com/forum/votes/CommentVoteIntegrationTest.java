package com.forum.votes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.posts.Post;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

class CommentVoteIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    private User commenter;
    private User voter;
    private Post post;
    private Long commentId;

    @BeforeEach
    void setUp() throws Exception {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        User author = users.create("author", Role.USER, UserStatus.ACTIVE);
        commenter = users.create("commenter", Role.USER, UserStatus.ACTIVE);
        voter = users.create("voter", Role.USER, UserStatus.ACTIVE);
        users.create("suspended", Role.USER, UserStatus.SUSPENDED);
        post = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, categories.create("Interior"));
        commentId = createComment(post.getId(), "commenter", "un comentario");
    }

    private Long createComment(Long postId, String username, String content) throws Exception {
        String body = mvc.perform(post("/posts/" + postId + "/comments").with(httpBasic(username, PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", content))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void vote_up_returnsScoreAndMyVote() throws Exception {
        mvc.perform(put("/comments/" + commentId + "/vote").with(httpBasic("voter", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.myVote").value("UPVOTE"));
    }

    @Test
    void vote_ownComment_returns403() throws Exception {
        mvc.perform(put("/comments/" + commentId + "/vote").with(httpBasic("commenter", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void vote_suspended_returns403() throws Exception {
        mvc.perform(put("/comments/" + commentId + "/vote").with(httpBasic("suspended", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void vote_anonymous_returns401() throws Exception {
        mvc.perform(put("/comments/" + commentId + "/vote")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vote_onHiddenComment_returns409() throws Exception {
        mvc.perform(patch("/comments/" + commentId + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk());
        mvc.perform(put("/comments/" + commentId + "/vote").with(httpBasic("voter", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isConflict());
    }

    @Test
    void vote_onCommentOfHiddenPost_returns404() throws Exception {
        mvc.perform(patch("/posts/" + post.getId() + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk());
        mvc.perform(put("/comments/" + commentId + "/vote").with(httpBasic("voter", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void commentList_reflectsScoreAndMyVote() throws Exception {
        votes.voteComment(voter.getId(), commentId, VoteValue.UPVOTE);
        mvc.perform(get("/posts/" + post.getId() + "/comments").with(httpBasic("voter", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(1))
                .andExpect(jsonPath("$[0].myVote").value("UPVOTE"));
    }

    @Test
    void hiddenComment_keepsScore() throws Exception {
        votes.voteComment(voter.getId(), commentId, VoteValue.UPVOTE);
        mvc.perform(patch("/comments/" + commentId + "/status").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", "HIDDEN"))))
                .andExpect(status().isOk());
        // el moderador ve el comentario oculto y su score se conserva (los votos no se borran)
        mvc.perform(get("/posts/" + post.getId() + "/comments").with(httpBasic("mod", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("HIDDEN"))
                .andExpect(jsonPath("$[0].score").value(1));
    }
}

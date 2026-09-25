package com.forum.votes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

class PostVoteIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    private User author;
    private User user2;
    private User user3;
    private Post post;

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        author = users.create("author", Role.USER, UserStatus.ACTIVE);
        user2 = users.create("user2", Role.USER, UserStatus.ACTIVE);
        user3 = users.create("user3", Role.USER, UserStatus.ACTIVE);
        users.create("suspended", Role.USER, UserStatus.SUSPENDED);
        post = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, categories.create("Interior"));
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    @Test
    void vote_up_returnsScoreAndMyVote() throws Exception {
        mvc.perform(put("/posts/" + post.getId() + "/vote").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(1))
                .andExpect(jsonPath("$.myVote").value("UPVOTE"));
    }

    @Test
    void vote_changeValue_updatesExistingVote() throws Exception {
        votes.votePost(user2.getId(), post.getId(), VoteValue.UPVOTE);
        mvc.perform(put("/posts/" + post.getId() + "/vote").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "DOWNVOTE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(-1))
                .andExpect(jsonPath("$.myVote").value("DOWNVOTE"));
    }

    @Test
    void delete_isIdempotent_evenIfNotVoted() throws Exception {
        mvc.perform(delete("/posts/" + post.getId() + "/vote").with(httpBasic("user2", PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_removesVote() throws Exception {
        votes.votePost(user2.getId(), post.getId(), VoteValue.UPVOTE);
        mvc.perform(delete("/posts/" + post.getId() + "/vote").with(httpBasic("user2", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/posts/" + post.getId()).with(httpBasic("user2", PASSWORD)))
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.myVote").doesNotExist());
    }

    @Test
    void vote_ownPost_returns403_whenSelfVoteDisabled() throws Exception {
        mvc.perform(put("/posts/" + post.getId() + "/vote").with(httpBasic("author", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void vote_anonymous_returns401() throws Exception {
        mvc.perform(put("/posts/" + post.getId() + "/vote")
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void vote_suspended_returns403() throws Exception {
        mvc.perform(put("/posts/" + post.getId() + "/vote").with(httpBasic("suspended", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void vote_onHiddenPost_asAnonymousUser_returns404() throws Exception {
        Post hidden = posts.create(author, PostType.QUESTION, ContentStatus.HIDDEN, categories.create("Exterior"));
        mvc.perform(put("/posts/" + hidden.getId() + "/vote").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void vote_onHiddenPost_asModerator_returns409() throws Exception {
        Post hidden = posts.create(author, PostType.QUESTION, ContentStatus.HIDDEN, categories.create("Genéticas"));
        mvc.perform(put("/posts/" + hidden.getId() + "/vote").with(httpBasic("mod", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("value", "UPVOTE"))))
                .andExpect(status().isConflict());
    }

    @Test
    void postResponse_reflectsScoreCommentCountAndMyVote() throws Exception {
        votes.votePost(user2.getId(), post.getId(), VoteValue.UPVOTE);
        votes.votePost(user3.getId(), post.getId(), VoteValue.UPVOTE);
        mvc.perform(post("/posts/" + post.getId() + "/comments").with(httpBasic("user2", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("content", "buen aporte"))))
                .andExpect(status().isCreated());

        mvc.perform(get("/posts/" + post.getId()).with(httpBasic("user2", PASSWORD)))
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.myVote").value("UPVOTE"));
    }
}

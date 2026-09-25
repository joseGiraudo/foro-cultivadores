package com.forum.follows;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.forum.IntegrationTestSupport;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

class FollowIntegrationTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;

    private User user1;
    private User user2;
    private User grow;

    @BeforeEach
    void setUp() {
        users.create("mod", Role.MODERATOR, UserStatus.ACTIVE);
        user1 = users.create("user1", Role.USER, UserStatus.ACTIVE);
        user2 = users.create("user2", Role.USER, UserStatus.ACTIVE);
        grow = users.create("grow", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        users.create("suspended", Role.USER, UserStatus.SUSPENDED);
    }

    @Test
    void follow_returns204_andReflectsInCountsAndListings() throws Exception {
        mvc.perform(post("/users/" + grow.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(get("/users/" + grow.getId()).with(httpBasic("user1", PASSWORD)))
                .andExpect(jsonPath("$.followerCount").value(1))
                .andExpect(jsonPath("$.followedByMe").value(true));

        mvc.perform(get("/users/" + user1.getId()))
                .andExpect(jsonPath("$.followingCount").value(1));

        mvc.perform(get("/users/" + grow.getId() + "/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].username").value("user1"))
                .andExpect(jsonPath("$.content[0].email").doesNotExist());

        mvc.perform(get("/users/" + user1.getId() + "/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("grow"));
    }

    @Test
    void follow_isIdempotent() throws Exception {
        mvc.perform(post("/users/" + grow.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/users/" + grow.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/users/" + grow.getId()))
                .andExpect(jsonPath("$.followerCount").value(1));
    }

    @Test
    void unfollow_returns204_andReducesCount() throws Exception {
        mvc.perform(post("/users/" + grow.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/users/" + grow.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/users/" + grow.getId()))
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void unfollow_whenNotFollowing_isIdempotent204() throws Exception {
        mvc.perform(delete("/users/" + grow.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNoContent());
    }

    @Test
    void follow_self_returns400() throws Exception {
        mvc.perform(post("/users/" + user1.getId() + "/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void follow_targetBelowMinRole_returns400() throws Exception {
        // user2 es USER; follow-target-min-role es CONTRIBUTOR, así que no se lo puede seguir.
        mvc.perform(post("/users/" + user2.getId() + "/follow").with(httpBasic("grow", PASSWORD)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void follow_nonExistentUser_returns404() throws Exception {
        mvc.perform(post("/users/999999/follow").with(httpBasic("user1", PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void follow_anonymous_returns401() throws Exception {
        mvc.perform(post("/users/" + grow.getId() + "/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void follow_suspended_returns403() throws Exception {
        mvc.perform(post("/users/" + grow.getId() + "/follow").with(httpBasic("suspended", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void profile_followedByMe_isNullForAnonymous() throws Exception {
        mvc.perform(get("/users/" + grow.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followedByMe").doesNotExist());
    }
}

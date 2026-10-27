package com.forum.follows;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.forum.TestcontainersConfiguration;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

/**
 * Con follow-target-min-role = USER (solo por config), se puede seguir a cualquier usuario, incluso
 * a un USER. Demuestra que la config modifica el comportamiento sin tocar código.
 */
@SpringBootTest(properties = {
        "forum.rules.follow-target-min-role=USER",
        "forum.security.http-basic-enabled=true"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestUsers.class})
@Transactional
class FollowTargetOverrideTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Test
    void canFollowRegularUser_whenTargetMinRoleLowered() throws Exception {
        users.create("follower", Role.USER, UserStatus.ACTIVE);
        User target = users.create("target", Role.USER, UserStatus.ACTIVE);

        mvc.perform(post("/users/" + target.getId() + "/follow").with(httpBasic("follower", TestUsers.PASSWORD)))
                .andExpect(status().isNoContent());
    }
}

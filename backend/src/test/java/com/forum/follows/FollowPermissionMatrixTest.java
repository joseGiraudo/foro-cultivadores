package com.forum.follows;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.forum.IntegrationTestSupport;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

/**
 * Matriz de permisos de follow (follow-min-role = USER) por actor. El objetivo es un CONTRIBUTOR
 * (seguible). Suspendido 403; anónimo 401.
 */
class FollowPermissionMatrixTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;
    private static final String ANONYMOUS = "";

    private User target;

    @BeforeEach
    void setUp() {
        users.create("f_user", Role.USER, UserStatus.ACTIVE);
        users.create("f_contributor", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        users.create("f_moderator", Role.MODERATOR, UserStatus.ACTIVE);
        users.create("f_admin", Role.ADMIN, UserStatus.ACTIVE);
        users.create("f_suspended", Role.USER, UserStatus.SUSPENDED);
        target = users.create("f_target", Role.CONTRIBUTOR, UserStatus.ACTIVE);
    }

    static Stream<Arguments> actors() {
        return Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("f_user", 204),
                Arguments.of("f_contributor", 204),
                Arguments.of("f_moderator", 204),
                Arguments.of("f_admin", 204),
                Arguments.of("f_suspended", 403));
    }

    @ParameterizedTest(name = "POST follow actor={0} -> {1}")
    @MethodSource("actors")
    void follow(String actor, int expected) throws Exception {
        var request = post("/users/" + target.getId() + "/follow");
        if (!ANONYMOUS.equals(actor)) {
            request = request.with(httpBasic(actor, PASSWORD));
        }
        mvc.perform(request).andExpect(status().is(expected));
    }
}

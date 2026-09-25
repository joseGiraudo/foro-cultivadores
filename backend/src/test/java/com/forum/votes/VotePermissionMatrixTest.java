package com.forum.votes;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.posts.Post;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;

/**
 * Matriz de permisos de votos (post-vote-min-role y comment-vote-min-role = USER) por actor.
 * Ningún actor es el autor, así que no interviene la regla de auto-voto. Suspendido 403; anónimo 401.
 */
class VotePermissionMatrixTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;
    private static final String ANONYMOUS = "";

    private Post post;
    private Long commentId;

    @BeforeEach
    void setUp() throws Exception {
        User author = users.create("v_author", Role.USER, UserStatus.ACTIVE);
        users.create("v_user", Role.USER, UserStatus.ACTIVE);
        users.create("v_contributor", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        users.create("v_moderator", Role.MODERATOR, UserStatus.ACTIVE);
        users.create("v_admin", Role.ADMIN, UserStatus.ACTIVE);
        users.create("v_suspended", Role.USER, UserStatus.SUSPENDED);
        post = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, categories.create("Interior"));

        String body = mvc.perform(post("/posts/" + post.getId() + "/comments").with(httpBasic("v_author", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "comentario del autor"))))
                .andReturn().getResponse().getContentAsString();
        commentId = objectMapper.readTree(body).get("id").asLong();
    }

    static Stream<Arguments> actors() {
        return Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("v_user", 200),
                Arguments.of("v_contributor", 200),
                Arguments.of("v_moderator", 200),
                Arguments.of("v_admin", 200),
                Arguments.of("v_suspended", 403));
    }

    @ParameterizedTest(name = "PUT post vote actor={0} -> {1}")
    @MethodSource("actors")
    void votePost(String actor, int expected) throws Exception {
        var request = put("/posts/" + post.getId() + "/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("value", "UPVOTE")));
        if (!ANONYMOUS.equals(actor)) {
            request = request.with(httpBasic(actor, PASSWORD));
        }
        mvc.perform(request).andExpect(status().is(expected));
    }

    @ParameterizedTest(name = "PUT comment vote actor={0} -> {1}")
    @MethodSource("actors")
    void voteComment(String actor, int expected) throws Exception {
        var request = put("/comments/" + commentId + "/vote")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("value", "UPVOTE")));
        if (!ANONYMOUS.equals(actor)) {
            request = request.with(httpBasic(actor, PASSWORD));
        }
        mvc.perform(request).andExpect(status().is(expected));
    }
}

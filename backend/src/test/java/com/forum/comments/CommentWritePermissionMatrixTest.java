package com.forum.comments;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

/**
 * Matriz de permisos de creación de comentarios (comment-min-role = USER) por actor.
 * Todos los roles activos pueden comentar; suspendido 403; anónimo 401.
 */
class CommentWritePermissionMatrixTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;
    private static final String ANONYMOUS = "";

    private Post post;

    @BeforeEach
    void setUp() {
        User author = users.create("c_author", Role.USER, UserStatus.ACTIVE);
        users.create("c_user", Role.USER, UserStatus.ACTIVE);
        users.create("c_contributor", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        users.create("c_moderator", Role.MODERATOR, UserStatus.ACTIVE);
        users.create("c_admin", Role.ADMIN, UserStatus.ACTIVE);
        users.create("c_suspended", Role.USER, UserStatus.SUSPENDED);
        Category interior = categories.create("Interior");
        post = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, interior);
    }

    static Stream<Arguments> actors() {
        return Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("c_user", 201),
                Arguments.of("c_contributor", 201),
                Arguments.of("c_moderator", 201),
                Arguments.of("c_admin", 201),
                Arguments.of("c_suspended", 403));
    }

    @ParameterizedTest(name = "POST comment actor={0} -> {1}")
    @MethodSource("actors")
    void createComment(String actor, int expected) throws Exception {
        var request = post("/posts/" + post.getId() + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("content", "un comentario")));
        if (!ANONYMOUS.equals(actor)) {
            request = request.with(httpBasic(actor, PASSWORD));
        }
        mvc.perform(request).andExpect(status().is(expected));
    }
}

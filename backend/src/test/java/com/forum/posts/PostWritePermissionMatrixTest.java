package com.forum.posts;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import com.forum.IntegrationTestSupport;
import com.forum.categories.Category;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.UserStatus;

/**
 * Matriz de permisos de creación de posts (acción con rol mínimo configurable) por actor.
 * QUESTION exige USER; ARTICLE exige CONTRIBUTOR. Suspendido siempre 403; anónimo 401.
 */
class PostWritePermissionMatrixTest extends IntegrationTestSupport {

    private static final String PASSWORD = TestUsers.PASSWORD;
    private static final String ANONYMOUS = "";

    private Category interior;

    @BeforeEach
    void setUp() {
        users.create("p_user", Role.USER, UserStatus.ACTIVE);
        users.create("p_contributor", Role.CONTRIBUTOR, UserStatus.ACTIVE);
        users.create("p_moderator", Role.MODERATOR, UserStatus.ACTIVE);
        users.create("p_admin", Role.ADMIN, UserStatus.ACTIVE);
        users.create("p_suspended", Role.USER, UserStatus.SUSPENDED);
        interior = categories.create("Interior");
    }

    private void createPost(String actor, String postType, int expected) throws Exception {
        var request = post("/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "title", "Título de prueba", "content", "Contenido",
                        "postType", postType, "categoryIds", List.of(interior.getId()))));
        if (!ANONYMOUS.equals(actor)) {
            request = request.with(httpBasic(actor, PASSWORD));
        }
        mvc.perform(request).andExpect(status().is(expected));
    }

    static Stream<Arguments> questionActors() {
        return Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("p_user", 201),
                Arguments.of("p_contributor", 201),
                Arguments.of("p_moderator", 201),
                Arguments.of("p_admin", 201),
                Arguments.of("p_suspended", 403));
    }

    @ParameterizedTest(name = "POST question actor={0} -> {1}")
    @MethodSource("questionActors")
    void createQuestion(String actor, int expected) throws Exception {
        createPost(actor, "QUESTION", expected);
    }

    static Stream<Arguments> articleActors() {
        return Stream.of(
                Arguments.of(ANONYMOUS, 401),
                Arguments.of("p_user", 403),
                Arguments.of("p_contributor", 201),
                Arguments.of("p_moderator", 201),
                Arguments.of("p_admin", 201),
                Arguments.of("p_suspended", 403));
    }

    @ParameterizedTest(name = "POST article actor={0} -> {1}")
    @MethodSource("articleActors")
    void createArticle(String actor, int expected) throws Exception {
        createPost(actor, "ARTICLE", expected);
    }
}

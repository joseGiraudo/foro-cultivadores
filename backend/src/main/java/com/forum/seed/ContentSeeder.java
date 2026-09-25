package com.forum.seed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.forum.categories.CategoryResponse;
import com.forum.categories.CategoryService;
import com.forum.comments.CommentResponse;
import com.forum.comments.CommentService;
import com.forum.comments.CreateCommentRequest;
import com.forum.images.ImageService;
import com.forum.posts.CreatePostRequest;
import com.forum.posts.PostResponse;
import com.forum.posts.PostService;
import com.forum.posts.UpdatePostRequest;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;
import com.forum.users.AuthUser;
import com.forum.users.User;
import com.forum.users.UserService;
import com.forum.votes.CommentVoteService;
import com.forum.votes.PostVoteService;
import com.forum.follows.FollowService;

/**
 * Carga el contenido de ejemplo (perfil {@code dev}, solo si todavía no hay posts) para que los
 * cuatro escenarios de la sección 8 del spec se vean funcionando al arrancar. Usa los mismos
 * services que la API real (por eso las imágenes pasan por el mismo ImageStorage).
 */
@Component
@Profile("dev")
@Order(3)
class ContentSeeder implements ApplicationRunner {

    private final UserService userService;
    private final CategoryService categoryService;
    private final PostService postService;
    private final CommentService commentService;
    private final PostVoteService postVoteService;
    private final CommentVoteService commentVoteService;
    private final FollowService followService;
    private final ImageService imageService;

    ContentSeeder(UserService userService, CategoryService categoryService, PostService postService,
                  CommentService commentService, PostVoteService postVoteService,
                  CommentVoteService commentVoteService, FollowService followService, ImageService imageService) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.postService = postService;
        this.commentService = commentService;
        this.postVoteService = postVoteService;
        this.commentVoteService = commentVoteService;
        this.followService = followService;
        this.imageService = imageService;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = userService.getByUsername("admin");
        AuthUser adminAuth = auth(admin);

        // Solo sembrar si no hay posts todavía.
        if (postService.list(null, null, null, null, adminAuth, PageRequest.of(0, 1)).getTotalElements() > 0) {
            return;
        }

        AuthUser mod = auth(userService.getByUsername("mod"));
        User growNorteUser = userService.getByUsername("grow_norte");
        User growSurUser = userService.getByUsername("grow_sur");
        AuthUser growNorte = auth(growNorteUser);
        AuthUser growSur = auth(growSurUser);
        AuthUser user1 = auth(userService.getByUsername("user1"));
        AuthUser user2 = auth(userService.getByUsername("user2"));
        AuthUser user3 = auth(userService.getByUsername("user3"));

        Map<String, Long> categories = categoryService.list().stream()
                .collect(Collectors.toMap(CategoryResponse::name, CategoryResponse::id));
        long interior = categories.get("Interior");
        long geneticas = categories.get("Genéticas");
        long riego = categories.get("Riego y nutrientes");

        byte[] sampleImage = readSampleImage();

        seedQuestionScenario(user1, user2, user3, growNorte, mod, interior, riego, sampleImage);
        seedArticleScenario(growNorte, user1, user2, user3, geneticas, sampleImage);
        seedGuideScenario(growSur, user1, user2, interior);
        seedFollows(user1, user2, user3, growNorteUser, growSurUser);
        seedHiddenContent(user2, mod, interior);
    }

    // Escenario 1: una QUESTION con imagen y respuestas con distinto score (para ver el orden por votos).
    private void seedQuestionScenario(AuthUser user1, AuthUser user2, AuthUser user3, AuthUser growNorte,
                                      AuthUser mod, long interior, long riego, byte[] sampleImage) {
        PostResponse question = postService.create(user1, new CreatePostRequest(
                "¿Por qué se ponen amarillas las hojas de mi planta?",
                "Hola, soy nuevo cultivando en interior. Las hojas de abajo se están poniendo amarillas. "
                        + "Adjunto una foto. ¿Qué puede ser?",
                PostType.QUESTION, List.of(interior, riego)));
        imageService.upload(question.id(), user1, sampleImage);

        CommentResponse a1 = commentService.create(question.id(), growNorte,
                new CreateCommentRequest("Puede ser deficiencia de nitrógeno. Revisá la EC del riego.", null));
        CommentResponse a2 = commentService.create(question.id(), user2,
                new CreateCommentRequest("A mí me pasó por exceso de agua, dejá secar el sustrato.", null));
        CommentResponse a3 = commentService.create(question.id(), user3,
                new CreateCommentRequest("Fijate el pH; si está alto la planta no absorbe nutrientes.", null));
        commentService.create(question.id(), user1,
                new CreateCommentRequest("¡Gracias! Voy a medir el pH esta tarde.", a3.id()));

        // Scores: a1 = +3, a3 = +2, a2 = -1 (así el orden por SCORE queda a1, a3, a2).
        commentVoteService.vote(a1.id(), user1, VoteValue.UPVOTE);
        commentVoteService.vote(a1.id(), user2, VoteValue.UPVOTE);
        commentVoteService.vote(a1.id(), user3, VoteValue.UPVOTE);
        commentVoteService.vote(a3.id(), growNorte, VoteValue.UPVOTE);
        commentVoteService.vote(a3.id(), user2, VoteValue.UPVOTE);
        commentVoteService.vote(a2.id(), user3, VoteValue.DOWNVOTE);

        postVoteService.vote(question.id(), growNorte, VoteValue.UPVOTE);
        postVoteService.vote(question.id(), user2, VoteValue.UPVOTE);

        // Un comentario oculto dentro de un hilo visible (para probar la visibilidad).
        CommentResponse offTopic = commentService.create(question.id(), user2,
                new CreateCommentRequest("comentario fuera de tema que será ocultado", null));
        commentService.changeStatus(offTopic.id(), mod, ContentStatus.HIDDEN);
    }

    // Escenario 2: un ARTICLE con imágenes referenciadas en el Markdown y votos positivos y negativos.
    private void seedArticleScenario(AuthUser growNorte, AuthUser user1, AuthUser user2, AuthUser user3,
                                     long geneticas, byte[] sampleImage) {
        PostResponse article = postService.create(growNorte, new CreatePostRequest(
                "Reseña: nueva genética \"Norte OG\"",
                "Reseña en preparación.",
                PostType.ARTICLE, List.of(geneticas)));
        long img1 = imageService.upload(article.id(), growNorte, sampleImage).id();
        long img2 = imageService.upload(article.id(), growNorte, sampleImage).id();

        postService.update(article.id(), growNorte, new UpdatePostRequest(null,
                "## Norte OG\n\nUna genética equilibrada, ideal para interior.\n\n"
                        + "![Planta en vegetativo](/images/" + img1 + ")\n\n"
                        + "![Cogollo en floración](/images/" + img2 + ")\n\n"
                        + "Rinde muy bien y es resistente a hongos. Recomendada.",
                null));

        postVoteService.vote(article.id(), user1, VoteValue.UPVOTE);
        postVoteService.vote(article.id(), user2, VoteValue.UPVOTE);
        postVoteService.vote(article.id(), user3, VoteValue.DOWNVOTE);
    }

    // Escenario 3: una GUIDE con pasos numerados y comentarios con dudas (orden cronológico).
    private void seedGuideScenario(AuthUser growSur, AuthUser user1, AuthUser user2, long interior) {
        PostResponse guide = postService.create(growSur, new CreatePostRequest(
                "Guía para principiantes: tu primer cultivo indoor",
                "# Primeros pasos\n\n1. Elegí una genética fácil (autofloreciente).\n"
                        + "2. Preparás un buen sustrato.\n3. Controlás luz, riego y ventilación.\n"
                        + "4. Cosechás y curás con paciencia.",
                PostType.GUIDE, List.of(interior)));
        commentService.create(guide.id(), user1,
                new CreateCommentRequest("¿Cuántas horas de luz conviene en vegetativo?", null));
        commentService.create(guide.id(), user2,
                new CreateCommentRequest("¿Qué tamaño de maceta recomiendan para empezar?", null));
    }

    // Escenario 4: usuarios siguiendo a grow_norte y grow_sur.
    private void seedFollows(AuthUser user1, AuthUser user2, AuthUser user3, User growNorte, User growSur) {
        followService.follow(growNorte.getId(), user1);
        followService.follow(growNorte.getId(), user2);
        followService.follow(growSur.getId(), user3);
        followService.follow(growSur.getId(), user1);
    }

    // Escenario 5 (post oculto): el comentario oculto se siembra en el escenario 1.
    private void seedHiddenContent(AuthUser user2, AuthUser mod, long interior) {
        PostResponse spam = postService.create(user2, new CreatePostRequest(
                "Compren seguidores baratos aquí",
                "Mensaje de spam que un moderador debería ocultar.",
                PostType.QUESTION, List.of(interior)));
        postService.changeStatus(spam.id(), mod, ContentStatus.HIDDEN);
    }

    private AuthUser auth(User user) {
        return new AuthUser(user.getId(), user.getUsername(), user.getPassword(), user.getRole(), user.getStatus());
    }

    private byte[] readSampleImage() {
        try {
            return new ClassPathResource("seed-images/sample.png").getContentAsByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen de ejemplo del seeder", e);
        }
    }
}

package com.forum.comments;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import com.forum.categories.TestCategories;
import com.forum.posts.Post;
import com.forum.posts.TestPosts;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;
import com.forum.users.Role;
import com.forum.users.TestUsers;
import com.forum.users.User;
import com.forum.users.UserStatus;
import com.forum.votes.TestVotes;

/**
 * Cambiando comment-order.QUESTION a CHRONOLOGICAL (solo por config), los comentarios de primer
 * nivel se ordenan por createdAt ascendente aunque sus scores difieran. Demuestra que la config
 * modifica el comportamiento sin tocar código.
 */
@SpringBootTest(properties = "forum.rules.comment-order.QUESTION=CHRONOLOGICAL")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestUsers.class, TestCategories.class, TestPosts.class, TestVotes.class})
@Transactional
class CommentOrderOverrideTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TestUsers users;

    @Autowired
    private TestCategories categories;

    @Autowired
    private TestPosts posts;

    @Autowired
    private TestVotes votes;

    @Autowired
    private CommentRepository commentRepository;

    private Post question;

    @BeforeEach
    void setUp() {
        User author = users.create("author", Role.USER, UserStatus.ACTIVE);
        question = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, categories.create("Interior"));
    }

    @Test
    void firstLevelCommentsOrderedChronologically_regardlessOfScore() throws Exception {
        User ua = users.create("ua", Role.USER, UserStatus.ACTIVE);
        User ub = users.create("ub", Role.USER, UserStatus.ACTIVE);
        User v1 = users.create("v1", Role.USER, UserStatus.ACTIVE);

        Comment cA = commentRepository.save(new Comment("cA", ua, question, null));
        Comment cB = commentRepository.save(new Comment("cB", ub, question, null));

        // cB tiene más score, pero con orden cronológico cA (creado antes) va primero.
        votes.voteComment(v1.getId(), cB.getId(), VoteValue.UPVOTE);

        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("cA"))
                .andExpect(jsonPath("$[1].content").value("cB"));
    }
}

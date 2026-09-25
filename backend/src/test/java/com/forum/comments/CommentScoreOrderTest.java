package com.forum.comments;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.forum.IntegrationTestSupport;
import com.forum.posts.Post;
import com.forum.shared.ContentStatus;
import com.forum.shared.PostType;
import com.forum.shared.VoteValue;
import com.forum.users.Role;
import com.forum.users.User;
import com.forum.users.UserStatus;

/**
 * Con comment-order = SCORE (el default para QUESTION), los comentarios de primer nivel se ordenan
 * por score descendente. Se siembran votos reales y se verifica el orden.
 */
class CommentScoreOrderTest extends IntegrationTestSupport {

    @Autowired
    private CommentRepository commentRepository;

    private Post question;

    @BeforeEach
    void setUp() {
        User author = users.create("author", Role.USER, UserStatus.ACTIVE);
        question = posts.create(author, PostType.QUESTION, ContentStatus.ACTIVE, categories.create("Interior"));
    }

    private Comment root(User author, String content) {
        return commentRepository.save(new Comment(content, author, question, null));
    }

    @Test
    void firstLevelCommentsOrderedByScoreDescending() throws Exception {
        User ua = users.create("ua", Role.USER, UserStatus.ACTIVE);
        User ub = users.create("ub", Role.USER, UserStatus.ACTIVE);
        User uc = users.create("uc", Role.USER, UserStatus.ACTIVE);
        User v1 = users.create("v1", Role.USER, UserStatus.ACTIVE);
        User v2 = users.create("v2", Role.USER, UserStatus.ACTIVE);

        Comment cA = root(ua, "cA");
        Comment cB = root(ub, "cB");
        Comment cC = root(uc, "cC");

        // cB = +2, cA = +1, cC = -1
        votes.voteComment(v1.getId(), cB.getId(), VoteValue.UPVOTE);
        votes.voteComment(v2.getId(), cB.getId(), VoteValue.UPVOTE);
        votes.voteComment(v1.getId(), cA.getId(), VoteValue.UPVOTE);
        votes.voteComment(v1.getId(), cC.getId(), VoteValue.DOWNVOTE);

        mvc.perform(get("/posts/" + question.getId() + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("cB")).andExpect(jsonPath("$[0].score").value(2))
                .andExpect(jsonPath("$[1].content").value("cA")).andExpect(jsonPath("$[1].score").value(1))
                .andExpect(jsonPath("$[2].content").value("cC")).andExpect(jsonPath("$[2].score").value(-1));
    }
}

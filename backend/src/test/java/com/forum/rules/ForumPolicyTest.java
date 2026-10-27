package com.forum.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.forum.shared.PostType;
import com.forum.users.Role;

class ForumPolicyTest {

    private ForumPolicy validPolicy() {
        ForumPolicy policy = new ForumPolicy();
        ForumPolicy.Rules rules = policy.getRules();
        for (PostType type : PostType.values()) {
            rules.getPostCreationMinRole().put(type, Role.USER);
            rules.getCommentOrder().put(type, CommentOrder.CHRONOLOGICAL);
        }
        rules.setCommentMinRole(Role.USER);
        rules.setPostVoteMinRole(Role.USER);
        rules.setCommentVoteMinRole(Role.USER);
        rules.setFollowMinRole(Role.USER);
        rules.setFollowTargetMinRole(Role.CONTRIBUTOR);
        rules.setModerationMinRole(Role.MODERATOR);
        rules.setBusinessProfileMinRole(Role.CONTRIBUTOR);
        rules.setCoverImageMinRole(Role.CONTRIBUTOR);
        policy.getImages().setMaxPerPost(10);
        policy.getImages().setAllowedTypes(List.of("image/jpeg"));
        policy.getStorage().getLocal().setBasePath("./data/uploads");
        return policy;
    }

    @Test
    void completeConfig_passesValidation() {
        assertThatCode(() -> validPolicy().afterPropertiesSet()).doesNotThrowAnyException();
    }

    @Test
    void missingPostCreationMinRole_failsAtStartup() {
        ForumPolicy policy = validPolicy();
        policy.getRules().getPostCreationMinRole().remove(PostType.GUIDE);
        assertThatThrownBy(policy::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("post-creation-min-role")
                .hasMessageContaining("GUIDE");
    }

    @Test
    void missingCommentOrder_failsAtStartup() {
        ForumPolicy policy = validPolicy();
        policy.getRules().getCommentOrder().remove(PostType.QUESTION);
        assertThatThrownBy(policy::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("comment-order")
                .hasMessageContaining("QUESTION");
    }

    @Test
    void missingModerationMinRole_failsAtStartup() {
        ForumPolicy policy = validPolicy();
        policy.getRules().setModerationMinRole(null);
        assertThatThrownBy(policy::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("moderation-min-role");
    }

    @Test
    void missingBasePath_failsAtStartup() {
        ForumPolicy policy = validPolicy();
        policy.getStorage().getLocal().setBasePath("  ");
        assertThatThrownBy(policy::afterPropertiesSet)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("base-path");
    }

    @Test
    void convenienceAccessors_returnConfiguredValues() {
        ForumPolicy policy = validPolicy();
        policy.getRules().getPostCreationMinRole().put(PostType.ARTICLE, Role.CONTRIBUTOR);
        policy.getRules().getCommentOrder().put(PostType.QUESTION, CommentOrder.SCORE);

        org.assertj.core.api.Assertions.assertThat(policy.postCreationMinRole(PostType.ARTICLE)).isEqualTo(Role.CONTRIBUTOR);
        org.assertj.core.api.Assertions.assertThat(policy.commentOrder(PostType.QUESTION)).isEqualTo(CommentOrder.SCORE);
        org.assertj.core.api.Assertions.assertThat(policy.moderationMinRole()).isEqualTo(Role.MODERATOR);
        org.assertj.core.api.Assertions.assertThat(policy.isSelfVoteAllowed()).isFalse();
    }
}

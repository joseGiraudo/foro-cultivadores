package com.forum.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.unit.DataSize;

import com.forum.IntegrationTestSupport;
import com.forum.shared.PostType;
import com.forum.users.Role;

/** Verifica que ForumPolicy se enlaza correctamente desde application.yml. */
class ForumPolicyBindingTest extends IntegrationTestSupport {

    @Autowired
    private ForumPolicy policy;

    @Test
    void bindsRulesFromYaml() {
        assertThat(policy.postCreationMinRole(PostType.QUESTION)).isEqualTo(Role.USER);
        assertThat(policy.postCreationMinRole(PostType.ARTICLE)).isEqualTo(Role.CONTRIBUTOR);
        assertThat(policy.postCreationMinRole(PostType.GUIDE)).isEqualTo(Role.CONTRIBUTOR);

        assertThat(policy.commentOrder(PostType.QUESTION)).isEqualTo(CommentOrder.SCORE);
        assertThat(policy.commentOrder(PostType.ARTICLE)).isEqualTo(CommentOrder.CHRONOLOGICAL);
        assertThat(policy.commentOrder(PostType.GUIDE)).isEqualTo(CommentOrder.CHRONOLOGICAL);

        assertThat(policy.commentMinRole()).isEqualTo(Role.USER);
        assertThat(policy.moderationMinRole()).isEqualTo(Role.MODERATOR);
        assertThat(policy.followTargetMinRole()).isEqualTo(Role.CONTRIBUTOR);
        assertThat(policy.isSelfVoteAllowed()).isFalse();
    }

    @Test
    void bindsImagesAndStorageFromYaml() {
        assertThat(policy.maxImagesPerPost()).isEqualTo(10);
        assertThat(policy.maxImageFileSize()).isEqualTo(DataSize.ofMegabytes(5));
        assertThat(policy.allowedImageTypes())
                .containsExactly("image/jpeg", "image/png", "image/webp");
        // base-path se sobreescribe a un directorio temporal en los tests (ver IntegrationTestSupport);
        // acá solo verificamos que quede configurado.
        assertThat(policy.imageStorageBasePath()).isNotBlank();
    }
}

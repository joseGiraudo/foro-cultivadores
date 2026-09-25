package com.forum.rules;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.forum.TestcontainersConfiguration;
import com.forum.shared.PostType;
import com.forum.users.Role;

/**
 * Demuestra que cambiar valores de {@code forum.rules} modifica el comportamiento sin tocar código:
 * acá se sobreescriben por propiedades y ForumPolicy los refleja. El efecto de estas reglas sobre
 * la creación de posts y el orden de comentarios se verifica en sus respectivos hitos.
 */
@SpringBootTest(properties = {
        "forum.rules.post-creation-min-role.QUESTION=CONTRIBUTOR",
        "forum.rules.comment-order.QUESTION=CHRONOLOGICAL",
        "forum.rules.follow-target-min-role=USER"
})
@Import(TestcontainersConfiguration.class)
class ForumPolicyOverrideTest {

    @Autowired
    private ForumPolicy policy;

    @Test
    void overriddenRulesTakeEffect() {
        assertThat(policy.postCreationMinRole(PostType.QUESTION)).isEqualTo(Role.CONTRIBUTOR);
        assertThat(policy.commentOrder(PostType.QUESTION)).isEqualTo(CommentOrder.CHRONOLOGICAL);
        assertThat(policy.followTargetMinRole()).isEqualTo(Role.USER);
    }
}

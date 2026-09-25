package com.forum.users;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void isAtLeast_sameRole_isTrue() {
        assertThat(Role.MODERATOR.isAtLeast(Role.MODERATOR)).isTrue();
    }

    @Test
    void isAtLeast_higherRole_isTrue() {
        assertThat(Role.ADMIN.isAtLeast(Role.USER)).isTrue();
        assertThat(Role.MODERATOR.isAtLeast(Role.CONTRIBUTOR)).isTrue();
    }

    @Test
    void isAtLeast_lowerRole_isFalse() {
        assertThat(Role.USER.isAtLeast(Role.CONTRIBUTOR)).isFalse();
        assertThat(Role.CONTRIBUTOR.isAtLeast(Role.ADMIN)).isFalse();
    }

    @Test
    void hierarchyOrder_isUserContributorModeratorAdmin() {
        assertThat(Role.values())
                .containsExactly(Role.USER, Role.CONTRIBUTOR, Role.MODERATOR, Role.ADMIN);
    }
}

package com.forum.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserTokenRepository extends JpaRepository<UserToken, Long> {

    Optional<UserToken> findByTokenHash(String tokenHash);

    /** Invalida tokens previos del mismo usuario y propósito al emitir uno nuevo. */
    void deleteByUserIdAndPurpose(Long userId, TokenPurpose purpose);
}

package com.forum.saves;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    Page<SavedPost> findByUserId(Long userId, Pageable pageable); // posts que guardó un usuario

    @Query("select s.postId from SavedPost s where s.userId = :userId and s.postId in :postIds")
    List<Long> savedAmong(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}

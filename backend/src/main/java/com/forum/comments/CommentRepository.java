package com.forum.comments;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forum.shared.ContentStatus;

interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostId(Long postId);

    @Query("""
            select c.post.id, count(c)
            from Comment c
            where c.post.id in :postIds and c.status = :status
            group by c.post.id
            """)
    List<Object[]> countByPostIdsAndStatus(@Param("postIds") Collection<Long> postIds,
                                           @Param("status") ContentStatus status);
}

package com.forum.votes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.forum.shared.VoteValue;

interface CommentVoteRepository extends JpaRepository<CommentVote, Long> {

    Optional<CommentVote> findByUserIdAndCommentId(Long userId, Long commentId);

    void deleteByUserIdAndCommentId(Long userId, Long commentId);

    @Query("""
            select v.commentId, sum(case when v.value = :up then 1 else -1 end)
            from CommentVote v
            where v.commentId in :commentIds
            group by v.commentId
            """)
    List<Object[]> scoreRows(@Param("commentIds") Collection<Long> commentIds, @Param("up") VoteValue up);

    @Query("select v.commentId, v.value from CommentVote v where v.userId = :userId and v.commentId in :commentIds")
    List<Object[]> myVoteRows(@Param("userId") Long userId, @Param("commentIds") Collection<Long> commentIds);
}

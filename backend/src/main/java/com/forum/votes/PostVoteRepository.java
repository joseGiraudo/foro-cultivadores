package com.forum.votes;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.forum.shared.VoteValue;

interface PostVoteRepository extends JpaRepository<PostVote, Long> {

    Optional<PostVote> findByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    @Query("""
            select v.postId, sum(case when v.value = :up then 1 else -1 end)
            from PostVote v
            where v.postId in :postIds
            group by v.postId
            """)
    List<Object[]> scoreRows(@Param("postIds") Collection<Long> postIds, @Param("up") VoteValue up);

    @Query("""
            select v.postId,
                   sum(case when v.value = :up then 1 else 0 end),
                   sum(case when v.value = :up then 0 else 1 end)
            from PostVote v
            where v.postId in :postIds
            group by v.postId
            """)
    List<Object[]> countRows(@Param("postIds") Collection<Long> postIds, @Param("up") VoteValue up);

    @Query("select v.postId, v.value from PostVote v where v.userId = :userId and v.postId in :postIds")
    List<Object[]> myVoteRows(@Param("userId") Long userId, @Param("postIds") Collection<Long> postIds);
}

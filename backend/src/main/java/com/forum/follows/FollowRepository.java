package com.forum.follows;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FollowRepository extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);

    Page<Follow> findByFollowingId(Long followingId, Pageable pageable); // seguidores de un usuario

    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);   // a quiénes sigue un usuario

    @Query("select f.followingId, count(f) from Follow f where f.followingId in :ids group by f.followingId")
    List<Object[]> followerCounts(@Param("ids") Collection<Long> ids);

    @Query("select f.followerId, count(f) from Follow f where f.followerId in :ids group by f.followerId")
    List<Object[]> followingCounts(@Param("ids") Collection<Long> ids);

    @Query("select f.followingId from Follow f where f.followerId = :followerId and f.followingId in :targetIds")
    List<Long> followingAmong(@Param("followerId") Long followerId, @Param("targetIds") Collection<Long> targetIds);
}

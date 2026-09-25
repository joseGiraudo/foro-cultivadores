package com.forum.images;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostIdOrderByPositionAsc(Long postId);

    long countByPostId(Long postId);

    @Query("select coalesce(max(i.position), -1) from PostImage i where i.postId = :postId")
    int maxPosition(@Param("postId") Long postId);

    Optional<PostImage> findByIdAndPostId(Long id, Long postId);

    /** Imagen de menor position por cada post (para la miniatura). */
    @Query("""
            select i from PostImage i
            where i.postId in :postIds
              and i.position = (select min(i2.position) from PostImage i2 where i2.postId = i.postId)
            """)
    List<PostImage> findThumbnails(@Param("postIds") Collection<Long> postIds);
}

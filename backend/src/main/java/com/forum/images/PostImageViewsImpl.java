package com.forum.images;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.posts.ImageResponse;
import com.forum.posts.PostImages;

/** Implementa la vista de imágenes que el módulo de posts necesita para armar sus respuestas. */
@Component
class PostImageViewsImpl implements PostImages {

    private final PostImageRepository postImageRepository;

    PostImageViewsImpl(PostImageRepository postImageRepository) {
        this.postImageRepository = postImageRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ImageResponse> imagesForPost(Long postId) {
        return postImageRepository.findByPostIdOrderByPositionAsc(postId).stream()
                .map(i -> ImageResponse.of(i.getId(), i.getPosition(), i.getContentType(), i.getSizeBytes()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> thumbnailUrlByPost(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> thumbnails = new LinkedHashMap<>();
        for (PostImage image : postImageRepository.findThumbnails(postIds)) {
            thumbnails.put(image.getPostId(), "/images/" + image.getId());
        }
        return thumbnails;
    }
}

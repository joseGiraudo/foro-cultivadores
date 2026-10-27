package com.forum.images;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.forum.images.ImageProcessor.ProcessedImage;
import com.forum.posts.ImageResponse;
import com.forum.posts.PostImageWriter;

/**
 * Implementa la escritura de imágenes que el módulo de posts necesita al crear un post con fotos.
 * Depende solo del procesador, el almacenamiento y el repositorio (NO de {@code PostService}), para
 * que {@code PostService} pueda inyectarlo sin generar un ciclo de beans.
 */
@Component
class PostImageWriterImpl implements PostImageWriter {

    private final PostImageRepository postImageRepository;
    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;

    PostImageWriterImpl(PostImageRepository postImageRepository, ImageStorage imageStorage,
                        ImageProcessor imageProcessor) {
        this.postImageRepository = postImageRepository;
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
    }

    @Override
    @Transactional
    public ImageResponse store(Long postId, byte[] content) {
        ProcessedImage processed = imageProcessor.process(content);
        int position = postImageRepository.maxPosition(postId) + 1;

        // Se escribe el archivo primero; si falla el guardado en base, se borra para no dejar huérfanos.
        String storageKey = imageStorage.store(processed.data(), processed.extension());
        try {
            PostImage saved = postImageRepository.save(new PostImage(
                    postId, storageKey, processed.contentType(), processed.data().length, position));
            return ImageResponse.of(saved.getId(), saved.getPosition(), saved.getContentType(), saved.getSizeBytes());
        } catch (RuntimeException e) {
            imageStorage.delete(storageKey);
            throw e;
        }
    }
}

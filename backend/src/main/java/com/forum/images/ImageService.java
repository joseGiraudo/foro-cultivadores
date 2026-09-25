package com.forum.images;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.forum.images.ImageProcessor.ProcessedImage;
import com.forum.posts.ImageResponse;
import com.forum.posts.Post;
import com.forum.posts.PostService;
import com.forum.rules.ForumPolicy;
import com.forum.shared.ContentStatus;
import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

@Service
public class ImageService {

    private final PostImageRepository postImageRepository;
    private final PostService postService;
    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;
    private final ForumPolicy policy;

    ImageService(PostImageRepository postImageRepository, PostService postService, ImageStorage imageStorage,
                 ImageProcessor imageProcessor, ForumPolicy policy) {
        this.postImageRepository = postImageRepository;
        this.postService = postService;
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
        this.policy = policy;
    }

    @Transactional
    ImageResponse upload(Long postId, AuthUser principal, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No se envió ningún archivo");
        }
        if (file.getSize() > policy.maxImageFileSize().toBytes()) {
            throw ApiException.payloadTooLarge("La imagen supera el tamaño máximo permitido");
        }
        return upload(postId, principal, readBytes(file));
    }

    /** Variante por bytes (usada por el seeder de contenido en perfil dev). */
    @Transactional
    public ImageResponse upload(Long postId, AuthUser principal, byte[] content) {
        principal.ensureCanWrite();
        requireEditablePost(postId, principal);
        if (postImageRepository.countByPostId(postId) >= policy.maxImagesPerPost()) {
            throw ApiException.conflict("El post alcanzó el máximo de imágenes permitidas");
        }

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

    @Transactional
    void delete(Long postId, Long imageId, AuthUser principal) {
        principal.ensureCanWrite();
        requireEditablePost(postId, principal);
        PostImage image = postImageRepository.findByIdAndPostId(imageId, postId)
                .orElseThrow(() -> ApiException.notFound("Imagen no encontrada"));
        postImageRepository.delete(image);
        imageStorage.delete(image.getStorageKey());
    }

    @Transactional(readOnly = true)
    ServedImage serve(Long imageId, AuthUser principal) {
        PostImage image = postImageRepository.findById(imageId)
                .orElseThrow(() -> ApiException.notFound("Imagen no encontrada"));
        // Misma visibilidad que el post: si no puede verlo, 404.
        postService.loadVisible(image.getPostId(), principal);
        Resource resource = imageStorage.load(image.getStorageKey());
        return new ServedImage(resource, image.getContentType());
    }

    private Post requireEditablePost(Long postId, AuthUser principal) {
        Post post = postService.loadVisible(postId, principal);
        if (!post.getAuthor().getId().equals(principal.getId())) {
            throw ApiException.forbidden("Solo el autor puede administrar las imágenes del post");
        }
        if (post.getStatus() != ContentStatus.ACTIVE) {
            throw ApiException.conflict("Solo se pueden administrar imágenes de un post activo");
        }
        return post;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer el archivo");
        }
    }

    record ServedImage(Resource resource, String contentType) {
    }
}

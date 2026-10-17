package com.forum.users;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.forum.images.ImageProcessor;
import com.forum.images.ImageProcessor.ProcessedImage;
import com.forum.images.ImageStorage;
import com.forum.rules.ForumPolicy;
import com.forum.shared.error.ApiException;

/**
 * Avatar y portada del usuario. Reutiliza el almacenamiento y el procesamiento de imágenes del
 * módulo {@code images} (que valida el tipo real y borra metadatos EXIF). La portada requiere un
 * rol mínimo configurable ({@code forum.rules.cover-image-min-role}); el avatar lo sube cualquiera.
 */
@Service
public class UserImageService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final ImageStorage imageStorage;
    private final ImageProcessor imageProcessor;
    private final ForumPolicy policy;

    UserImageService(UserRepository userRepository, UserService userService, ImageStorage imageStorage,
                     ImageProcessor imageProcessor, ForumPolicy policy) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.imageStorage = imageStorage;
        this.imageProcessor = imageProcessor;
        this.policy = policy;
    }

    @Transactional
    public UserResponse uploadAvatar(AuthUser principal, MultipartFile file) {
        User user = loadWritable(principal);
        ProcessedImage processed = process(file);
        String previousKey = user.getAvatarStorageKey();
        String storageKey = imageStorage.store(processed.data(), processed.extension());
        user.setAvatar(storageKey, processed.contentType());
        deleteQuietly(previousKey);
        return userService.getMe(principal);
    }

    @Transactional
    public UserResponse deleteAvatar(AuthUser principal) {
        User user = loadWritable(principal);
        String previousKey = user.getAvatarStorageKey();
        user.setAvatar(null, null);
        deleteQuietly(previousKey);
        return userService.getMe(principal);
    }

    @Transactional
    public UserResponse uploadCover(AuthUser principal, MultipartFile file) {
        User user = loadWritable(principal);
        if (!principal.getRole().isAtLeast(policy.coverImageMinRole())) {
            throw ApiException.forbidden("Tu rol no permite subir una portada");
        }
        ProcessedImage processed = process(file);
        String previousKey = user.getCoverStorageKey();
        String storageKey = imageStorage.store(processed.data(), processed.extension());
        user.setCover(storageKey, processed.contentType());
        deleteQuietly(previousKey);
        return userService.getMe(principal);
    }

    @Transactional
    public UserResponse deleteCover(AuthUser principal) {
        User user = loadWritable(principal);
        String previousKey = user.getCoverStorageKey();
        user.setCover(null, null);
        deleteQuietly(previousKey);
        return userService.getMe(principal);
    }

    @Transactional(readOnly = true)
    public ServedImage serveAvatar(Long userId) {
        User user = userService.getEntityById(userId);
        if (user.getAvatarStorageKey() == null) {
            throw ApiException.notFound("El usuario no tiene avatar");
        }
        return new ServedImage(imageStorage.load(user.getAvatarStorageKey()), user.getAvatarContentType());
    }

    @Transactional(readOnly = true)
    public ServedImage serveCover(Long userId) {
        User user = userService.getEntityById(userId);
        if (user.getCoverStorageKey() == null) {
            throw ApiException.notFound("El usuario no tiene portada");
        }
        return new ServedImage(imageStorage.load(user.getCoverStorageKey()), user.getCoverContentType());
    }

    private User loadWritable(AuthUser principal) {
        principal.ensureCanWrite();
        return userService.getEntityById(principal.getId());
    }

    private ProcessedImage process(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("No se envió ningún archivo");
        }
        if (file.getSize() > policy.maxImageFileSize().toBytes()) {
            throw ApiException.payloadTooLarge("La imagen supera el tamaño máximo permitido");
        }
        try {
            return imageProcessor.process(file.getBytes());
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer el archivo");
        }
    }

    private void deleteQuietly(String storageKey) {
        if (storageKey != null) {
            imageStorage.delete(storageKey);
        }
    }

    public record ServedImage(Resource resource, String contentType) {
    }
}

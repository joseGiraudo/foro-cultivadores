package com.forum.images;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.forum.rules.ForumPolicy;
import com.forum.shared.error.ApiException;

import jakarta.annotation.PostConstruct;

/**
 * Almacenamiento de imágenes en disco local. El nombre del archivo se genera (UUID + extensión);
 * nunca se usa el nombre enviado por el cliente. El directorio base se crea y valida al arrancar.
 */
@Component
class LocalDiskImageStorage implements ImageStorage {

    private final Path basePath;

    LocalDiskImageStorage(ForumPolicy policy) {
        this.basePath = Path.of(policy.imageStorageBasePath()).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(basePath);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de imágenes: " + basePath, e);
        }
        if (!Files.isWritable(basePath)) {
            throw new IllegalStateException("El directorio de imágenes no tiene permisos de escritura: " + basePath);
        }
    }

    @Override
    public String store(byte[] content, String extension) {
        String storageKey = UUID.randomUUID() + "." + extension;
        try {
            Files.write(basePath.resolve(storageKey), content);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen en disco", e);
        }
        return storageKey;
    }

    @Override
    public Resource load(String storageKey) {
        Path path = basePath.resolve(storageKey).normalize();
        if (!path.startsWith(basePath) || !Files.isReadable(path)) {
            throw ApiException.notFound("Imagen no encontrada");
        }
        return new PathResource(path);
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(basePath.resolve(storageKey).normalize());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo borrar la imagen en disco", e);
        }
    }
}

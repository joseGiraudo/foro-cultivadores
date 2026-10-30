package com.forum.images;

import java.net.URI;
import java.util.UUID;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.forum.shared.error.ApiException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Almacenamiento de imágenes en Cloudflare R2 (S3-compatible) vía el AWS SDK v2. La clave del objeto
 * se genera (UUID + extensión); nunca se usa el nombre del cliente. Se usa <b>path-style</b> para que
 * el mismo cliente funcione contra R2 (prod) y contra MinIO (tests). El content-type al servir sale
 * de la entidad {@code PostImage}; acá se guarda por prolijidad (útil si en el futuro se sirve por CDN).
 */
class R2ImageStorage implements ImageStorage, AutoCloseable {

    private final S3Client client;
    private final String bucket;

    R2ImageStorage(R2StorageProperties props) {
        Assert.hasText(props.getEndpoint(), "Falta forum.storage.r2.endpoint");
        Assert.hasText(props.getBucket(), "Falta forum.storage.r2.bucket");
        Assert.hasText(props.getAccessKey(), "Falta forum.storage.r2.access-key");
        Assert.hasText(props.getSecretKey(), "Falta forum.storage.r2.secret-key");
        this.bucket = props.getBucket();
        this.client = S3Client.builder()
                .endpointOverride(URI.create(props.getEndpoint()))
                .region(Region.of(props.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
                .forcePathStyle(true) // sirve para R2 y es obligatorio para MinIO (tests)
                .build();
    }

    @Override
    public String store(byte[] content, String extension) {
        String storageKey = UUID.randomUUID() + "." + extension;
        client.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(storageKey).contentType(contentTypeFor(extension)).build(),
                RequestBody.fromBytes(content));
        return storageKey;
    }

    @Override
    public Resource load(String storageKey) {
        try {
            byte[] bytes = client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucket).key(storageKey).build()).asByteArray();
            return new ByteArrayResource(bytes);
        } catch (NoSuchKeyException e) {
            throw ApiException.notFound("Imagen no encontrada");
        }
    }

    @Override
    public void delete(String storageKey) {
        // DeleteObject es idempotente: borrar una clave inexistente no falla.
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
    }

    @Override
    public void close() {
        client.close();
    }

    private static String contentTypeFor(String extension) {
        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}

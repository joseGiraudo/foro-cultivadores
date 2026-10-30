package com.forum.images;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config del almacenamiento de imágenes en Cloudflare R2 (S3-compatible), bajo {@code forum.storage.r2}.
 * Cuando {@code enabled=true}, las imágenes van a R2 en vez del disco local (ver {@link ImageStorageConfig}).
 * En dev/tests queda apagado → se usa {@link LocalDiskImageStorage}.
 */
@ConfigurationProperties(prefix = "forum.storage.r2")
public class R2StorageProperties {

    /** Interruptor: si es true se usa R2; si no, disco local. */
    private boolean enabled;

    /** Endpoint S3 de la cuenta R2: {@code https://<account-id>.r2.cloudflarestorage.com}. */
    private String endpoint;

    /** Nombre del bucket. */
    private String bucket;

    /** Access key del token de R2. */
    private String accessKey;

    /** Secret key del token de R2. */
    private String secretKey;

    /** Región para la firma S3. R2 recomienda {@code auto}; MinIO (tests) usa {@code us-east-1}. */
    private String region = "auto";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }
}

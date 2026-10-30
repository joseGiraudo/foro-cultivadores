package com.forum.images;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.forum.shared.error.ApiException;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Prueba {@link R2ImageStorage} contra un servidor S3-compatible real (adobe/s3mock, igual que R2).
 * Verifica el round-trip store → load → delete y que un load de una clave borrada da 404, con el
 * mismo código y config (path-style) que se usan contra R2 en prod. s3mock acepta cualquier credencial.
 */
@Testcontainers
class R2ImageStorageTest {

    private static final String BUCKET = "forum-test";

    @Container
    static final GenericContainer<?> S3MOCK = new GenericContainer<>("adobe/s3mock:latest")
            .withExposedPorts(9090);

    private R2ImageStorage storage;

    @BeforeEach
    void setUp() {
        String endpoint = "http://" + S3MOCK.getHost() + ":" + S3MOCK.getMappedPort(9090);

        // Crear el bucket con un cliente admin (R2ImageStorage no administra buckets).
        try (S3Client admin = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of("us-east-1"))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .forcePathStyle(true)
                .build()) {
            if (admin.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(BUCKET))) {
                admin.createBucket(b -> b.bucket(BUCKET));
            }
        }

        R2StorageProperties props = new R2StorageProperties();
        props.setEnabled(true);
        props.setEndpoint(endpoint);
        props.setBucket(BUCKET);
        props.setAccessKey("test"); // s3mock ignora las credenciales
        props.setSecretKey("test");
        props.setRegion("us-east-1");
        storage = new R2ImageStorage(props);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    @Test
    void storeLoadDelete_roundtrip() throws IOException {
        byte[] content = {1, 2, 3, 4, 5, 6, 7, 8};

        String key = storage.store(content, "png");
        assertThat(key).endsWith(".png");
        assertThat(storage.load(key).getContentAsByteArray()).isEqualTo(content);

        storage.delete(key);
        assertThatThrownBy(() -> storage.load(key)).isInstanceOf(ApiException.class);
    }

    @Test
    void delete_missingKey_isIdempotent() {
        storage.delete("no-existe.png"); // no debe lanzar
    }
}

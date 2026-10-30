package com.forum.images;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.forum.rules.ForumPolicy;

/**
 * Elige el {@link ImageStorage}: Cloudflare R2 si {@code forum.storage.r2.enabled=true}, si no el
 * disco local. Mismo patrón que {@code EmailConfig}: dev/tests funcionan sin nube (disco local), y en
 * prod se activa R2 por config sin tocar services ni controllers.
 */
@Configuration
class ImageStorageConfig {

    @Bean
    @ConditionalOnProperty(prefix = "forum.storage.r2", name = "enabled", havingValue = "true")
    ImageStorage r2ImageStorage(R2StorageProperties properties) {
        return new R2ImageStorage(properties);
    }

    @Bean
    @ConditionalOnMissingBean(ImageStorage.class)
    ImageStorage localDiskImageStorage(ForumPolicy policy) {
        return new LocalDiskImageStorage(policy);
    }
}

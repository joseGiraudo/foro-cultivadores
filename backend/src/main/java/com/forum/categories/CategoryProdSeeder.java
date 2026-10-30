package com.forum.categories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Siembra las categorías base en producción cuando {@code forum.bootstrap.enabled=true}. Sin esto, en
 * prod la tabla arranca vacía (el {@link CategorySeeder} de ejemplo es solo perfil {@code dev}) y
 * nadie podría publicar, porque el formulario exige elegir 1–4 categorías. Idempotente: inserta solo
 * las que falten (por nombre, case-insensitive), así se puede dejar activado y no duplica al reiniciar.
 */
@Component
@ConditionalOnProperty(prefix = "forum.bootstrap", name = "enabled", havingValue = "true")
@Order(2)
class CategoryProdSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoryProdSeeder.class);

    private final CategoryRepository categoryRepository;

    CategoryProdSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (String name : DefaultCategories.NAMES) {
            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                categoryRepository.save(new Category(name, null));
                created++;
            }
        }
        if (created > 0) {
            log.info("Bootstrap: sembradas {} categoría(s) base.", created);
        }
    }
}

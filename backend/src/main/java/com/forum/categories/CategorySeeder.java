package com.forum.categories;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Carga las categorías de ejemplo solo con el perfil {@code dev}. Es aditivo e idempotente: en cada
 * arranque inserta las que falten (por nombre, case-insensitive), así sumar categorías nuevas a la
 * lista las agrega también a una base existente al reiniciar, sin pisar ni duplicar las que ya están.
 */
@Component
@Profile("dev")
@Order(2)
class CategorySeeder implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        DefaultCategories.NAMES.stream()
                .filter(name -> !categoryRepository.existsByNameIgnoreCase(name))
                .forEach(name -> categoryRepository.save(new Category(name, null)));
    }
}

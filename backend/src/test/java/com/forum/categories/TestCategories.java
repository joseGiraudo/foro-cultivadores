package com.forum.categories;

import org.springframework.stereotype.Component;

/**
 * Helper de tests para crear categorías desde cualquier módulo. Vive en
 * {@code com.forum.categories} para usar el repositorio package-private y es público para
 * que otros paquetes de test lo inyecten.
 */
@Component
public class TestCategories {

    private final CategoryRepository categoryRepository;

    TestCategories(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category create(String name) {
        return categoryRepository.save(new Category(name, null));
    }
}

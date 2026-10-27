package com.forum.categories;

import java.util.List;

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

    private static final List<String> NAMES = List.of(
            "Interior", "Exterior", "Riego y nutrientes", "Plagas y enfermedades",
            "Equipamiento", "Genéticas", "Germinación", "Floración", "Cosecha y curado",
            "Iluminación", "Sustratos y macetas", "Cultivo orgánico", "Legales y normativa",
            "Muestra tu cultivo");

    private final CategoryRepository categoryRepository;

    CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        NAMES.stream()
                .filter(name -> !categoryRepository.existsByNameIgnoreCase(name))
                .forEach(name -> categoryRepository.save(new Category(name, null)));
    }
}

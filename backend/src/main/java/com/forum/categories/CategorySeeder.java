package com.forum.categories;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Carga las categorías de ejemplo solo con el perfil {@code dev} y solo si la tabla está vacía. */
@Component
@Profile("dev")
@Order(2)
class CategorySeeder implements ApplicationRunner {

    private static final List<String> NAMES = List.of(
            "Interior", "Exterior", "Riego y nutrientes",
            "Plagas y enfermedades", "Equipamiento", "Genéticas");

    private final CategoryRepository categoryRepository;

    CategorySeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) {
            return;
        }
        NAMES.forEach(name -> categoryRepository.save(new Category(name, null)));
    }
}

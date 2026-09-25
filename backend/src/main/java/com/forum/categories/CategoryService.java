package com.forum.categories;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.forum.shared.error.ApiException;
import com.forum.users.AuthUser;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(CategoryResponse::of)
                .toList();
    }

    @Transactional
    public CategoryResponse create(AuthUser admin, CreateCategoryRequest request) {
        admin.ensureCanWrite();
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw ApiException.conflict("Ya existe una categoría con ese nombre");
        }
        Category category = categoryRepository.save(new Category(request.name(), request.description()));
        return CategoryResponse.of(category);
    }

    /**
     * Resuelve las entidades {@link Category} para el conjunto de ids (sin duplicados).
     * Si alguna no existe, lanza 400. Pensado para que el módulo de posts arme sus categorías.
     */
    @Transactional(readOnly = true)
    public Set<Category> getEntitiesByIds(Collection<Long> ids) {
        Set<Long> distinct = new LinkedHashSet<>(ids);
        List<Category> found = categoryRepository.findAllById(distinct);
        if (found.size() != distinct.size()) {
            throw ApiException.badRequest("Alguna de las categorías indicadas no existe");
        }
        return new LinkedHashSet<>(found);
    }
}

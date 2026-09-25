package com.forum.categories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Category> findAllByOrderByNameAsc();
}

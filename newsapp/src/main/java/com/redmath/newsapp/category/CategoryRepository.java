package com.redmath.newsapp.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.concurrent.atomic.AtomicMarkableReference;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByName(String name);

}

package com.medikit.product.repository;

import com.medikit.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByActiveTrueOrderBySortOrderAsc();

    boolean existsByName(String name);
}

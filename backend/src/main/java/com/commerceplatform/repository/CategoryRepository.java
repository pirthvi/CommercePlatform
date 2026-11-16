package com.commerceplatform.repository;

import com.commerceplatform.domain.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CatalogCategory, Long> {
    List<CatalogCategory> findAllByOrderByNameAsc();
    Optional<CatalogCategory> findByNormalizedName(String name);
    boolean existsBySlug(String slug);
}

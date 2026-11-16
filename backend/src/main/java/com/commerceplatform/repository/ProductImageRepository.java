package com.commerceplatform.repository;

import com.commerceplatform.domain.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    List<ProductImage> findByProductIdAndVariantOrderByPositionAscIdAsc(Long productId, String variant);
    Optional<ProductImage> findByIdAndProductId(Long id, Long productId);
    void deleteByProductIdAndVariant(Long productId, String variant);
    void deleteByProductId(Long productId);
}

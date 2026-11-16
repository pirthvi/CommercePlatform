package com.commerceplatform.repository;

import com.commerceplatform.domain.ContentPageImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentPageImageRepository extends JpaRepository<ContentPageImage, Long> {
    Optional<ContentPageImage> findByIdAndPageId(Long id, Long pageId);
    void deleteByPageId(Long pageId);
}

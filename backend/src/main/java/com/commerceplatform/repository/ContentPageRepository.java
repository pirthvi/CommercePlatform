package com.commerceplatform.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import com.commerceplatform.domain.ContentPage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ContentPageRepository extends JpaRepository<ContentPage, Long> {
    Optional<ContentPage> findBySlugAndPublishedTrue(String slug);
    Optional<ContentPage> findBySlug(String slug);
    @Query("select p.layoutJson from ContentPage p where p.layoutJson is not null")
    List<String> findComponentLayouts();
    Optional<ContentPage> findByTemplateKey(String templateKey);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ContentPage p where p.id = :id")
    Optional<ContentPage> findForEditing(@Param("id") Long id);
    List<ContentPage> findByPublishedTrueOrderByTitleAsc();
}

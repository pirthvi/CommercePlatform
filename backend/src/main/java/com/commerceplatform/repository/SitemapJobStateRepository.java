package com.commerceplatform.repository;

import com.commerceplatform.domain.SitemapJobState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SitemapJobStateRepository extends JpaRepository<SitemapJobState, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from SitemapJobState state where state.id = :id")
    Optional<SitemapJobState> findForEditing(@Param("id") Long id);
}

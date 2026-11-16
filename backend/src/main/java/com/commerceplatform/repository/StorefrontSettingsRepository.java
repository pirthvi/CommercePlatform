package com.commerceplatform.repository;

import com.commerceplatform.domain.StorefrontSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface StorefrontSettingsRepository extends JpaRepository<StorefrontSettings, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StorefrontSettings s where s.id = 1")
    Optional<StorefrontSettings> findForEditing();
}

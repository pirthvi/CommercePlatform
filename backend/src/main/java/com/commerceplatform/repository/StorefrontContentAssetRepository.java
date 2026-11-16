package com.commerceplatform.repository;

import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import com.commerceplatform.domain.StorefrontContentAsset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorefrontContentAssetRepository extends JpaRepository<StorefrontContentAsset, String> {
    @Modifying
    @Query("delete from StorefrontContentAsset a where a.assetKey like concat(:prefix, '%')")
    void deleteByPrefix(@Param("prefix") String prefix);
    @Query("select a.assetKey from StorefrontContentAsset a where a.assetKey like 'pdp.%'")
    java.util.List<String> productIconKeys();
}

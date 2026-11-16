package com.commerceplatform.api;

import com.commerceplatform.domain.StorefrontContentAsset;
import com.commerceplatform.repository.StorefrontContentAssetRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api/content/site/assets")
public class StorefrontContentAssetController {
    private final StorefrontContentAssetRepository assets;

    public StorefrontContentAssetController(StorefrontContentAssetRepository assets) { this.assets = assets; }

    @Transactional(readOnly = true)
    @GetMapping("/{assetKey}")
    public ResponseEntity<byte[]> get(@PathVariable String assetKey) {
        StorefrontContentAsset asset = assets.findById(assetKey)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.getContentType()))
            .cacheControl(CacheControl.noCache()).body(asset.getData());
    }
}

package com.commerceplatform.api;

import com.commerceplatform.domain.StorefrontSettings;
import com.commerceplatform.repository.StorefrontSettingsRepository;
import com.commerceplatform.service.StorefrontContentMapper;
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

import java.time.Duration;

@RestController
@RequestMapping("/api/content/site")
public class StorefrontContentController {
    private final StorefrontSettingsRepository settings;
    private final StorefrontContentMapper mapper;

    public StorefrontContentController(StorefrontSettingsRepository settings, StorefrontContentMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public StorefrontContentResponse content() {
        return mapper.toResponse(settings.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)));
    }

    @Transactional(readOnly = true)
    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        StorefrontSettings store = settings.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (store.getLogoData() == null || store.getLogoContentType() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(store.getLogoContentType()))
            .cacheControl(CacheControl.noCache()).body(store.getLogoData());
    }
}

package com.commerceplatform.api;

import com.commerceplatform.domain.ContentPageImage;
import com.commerceplatform.repository.ContentPageImageRepository;
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
@RequestMapping("/api/content/pages")
public class ContentPageImageController {
    private final ContentPageImageRepository images;

    public ContentPageImageController(ContentPageImageRepository images) { this.images = images; }

    @Transactional(readOnly = true)
    @GetMapping("/{pageId}/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable Long pageId, @PathVariable Long imageId) {
        ContentPageImage image = images.findByIdAndPageId(imageId, pageId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.getContentType()))
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .body(image.getData());
    }
}

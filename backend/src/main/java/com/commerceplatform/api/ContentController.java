package com.commerceplatform.api;

import com.commerceplatform.domain.ContentPage;
import com.commerceplatform.repository.ContentPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {
    private final ContentPageRepository pages;
    public ContentController(ContentPageRepository pages) { this.pages = pages; }
    @GetMapping public List<ContentPage> list() { return pages.findByPublishedTrueOrderByTitleAsc(); }
    @GetMapping("/{slug}") public ContentPage get(@PathVariable String slug) {
        return pages.findBySlugAndPublishedTrue(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

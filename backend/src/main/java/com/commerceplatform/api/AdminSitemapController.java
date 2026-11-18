package com.commerceplatform.api;

import com.commerceplatform.service.SitemapJobService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sitemap")
public class AdminSitemapController {
    private final SitemapJobService sitemap;

    public AdminSitemapController(SitemapJobService sitemap) { this.sitemap = sitemap; }

    public record SettingsEdit(@NotBlank @Size(max = 300) String siteUrl) {}

    @GetMapping
    public SitemapJobService.Status status() { return sitemap.status(); }

    @PutMapping
    public SitemapJobService.Status configure(@Valid @RequestBody SettingsEdit edit) {
        return sitemap.configure(edit.siteUrl());
    }

    @PostMapping("/generate")
    public SitemapJobService.Status generate() { return sitemap.generateNow(); }
}

package com.commerceplatform.api;

import com.commerceplatform.service.SitemapJobService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicSitemapController {
    private final SitemapJobService sitemap;

    public PublicSitemapController(SitemapJobService sitemap) { this.sitemap = sitemap; }

    @GetMapping(value = {"/sitemap.xml", "/api/sitemap.xml"}, produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        String xml = sitemap.currentXml();
        if (xml == null) return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).cacheControl(CacheControl.noCache()).body(xml);
    }
}

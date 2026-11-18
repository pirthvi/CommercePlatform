package com.commerceplatform.service;

import com.commerceplatform.api.StoreProblem;
import com.commerceplatform.domain.ContentPage;
import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.SitemapJobState;
import com.commerceplatform.repository.ContentPageRepository;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.SitemapJobStateRepository;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.context.event.EventListener;

@Service
public class SitemapJobService {
    private static final Logger log = LoggerFactory.getLogger(SitemapJobService.class);
    private static final Set<String> ROUTE_TEMPLATES = Set.of("home", "shop");
    private static final Set<String> EXCLUDED_TEMPLATES = Set.of("cart", "checkout", "login", "register", "account", "order-success", "product");

    private final SitemapJobStateRepository states;
    private final ContentPageRepository pages;
    private final ProductRepository products;
    private final String cron;
    private final String defaultSiteUrl;

    public SitemapJobService(SitemapJobStateRepository states, ContentPageRepository pages, ProductRepository products,
                             @Value("${app.sitemap.cron:0 0 3 * * *}") String cron,
                             @Value("${app.sitemap.site-url:}") String defaultSiteUrl) {
        this.states = states;
        this.pages = pages;
        this.products = products;
        this.cron = cron;
        this.defaultSiteUrl = defaultSiteUrl;
    }

    public record Status(String siteUrl, String schedule, String timeZone, Instant nextRunAt,
                         Instant lastAttemptAt, Instant lastSuccessAt, Boolean lastRunSucceeded,
                         int urlCount, String lastError, String publicSitemapUrl) {}

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void generateOnStartup() {
        generateAndPersist();
    }

    @Scheduled(cron = "${app.sitemap.cron:0 0 3 * * *}", zone = "UTC")
    @Transactional
    public void generateScheduled() {
        generateAndPersist();
    }

    @Transactional
    public Status generateNow() {
        return generateAndPersist();
    }

    @Transactional
    public Status configure(String siteUrl) {
        String normalized = normalizeSiteUrl(siteUrl);
        SitemapJobState state = getForEditing();
        if (!normalized.equals(state.getSiteUrl())) {
            // Do not serve a sitemap containing URLs for the previous storefront domain.
            state.setSitemapXml(null);
            state.setUrlCount(0);
            state.setLastSuccessAt(null);
        }
        state.setSiteUrl(normalized);
        return generateAndPersist(state);
    }

    @Transactional
    public Status status() {
        SitemapJobState state = states.findById(1L).orElseGet(this::newState);
        return toStatus(state);
    }

    @Transactional
    public String currentXml() {
        return states.findById(1L).map(SitemapJobState::getSitemapXml).orElse(null);
    }

    private Status generateAndPersist() {
        return generateAndPersist(getForEditing());
    }

    private Status generateAndPersist(SitemapJobState state) {
        Instant now = Instant.now();
        state.setLastAttemptAt(now);
        state.setLastRunSucceeded(false);
        state.setLastError(null);
        try {
            String baseUrl = normalizeSiteUrl(state.getSiteUrl());
            state.setSiteUrl(baseUrl);
            Map<String, Instant> urls = collectUrls();
            String xml = toXml(baseUrl, urls);
            state.setSitemapXml(xml);
            state.setLastSuccessAt(now);
            state.setLastRunSucceeded(true);
            state.setUrlCount(urls.size());
        } catch (RuntimeException failure) {
            String reason = failure instanceof StoreProblem
                ? "Set a valid public storefront URL in Studio → Settings → Sitemap."
                : failure.getMessage() == null ? "Sitemap generation failed." : failure.getMessage();
            state.setLastError(reason.substring(0, Math.min(reason.length(), 1000)));
            log.warn("Sitemap generation failed: {}", reason, failure);
        }
        return toStatus(state);
    }

    private SitemapJobState getForEditing() {
        return states.findForEditing(1L).orElseGet(() -> states.save(newState()));
    }

    private SitemapJobState newState() {
        SitemapJobState state = new SitemapJobState();
        if (defaultSiteUrl != null && !defaultSiteUrl.isBlank()) {
            try { state.setSiteUrl(normalizeSiteUrl(defaultSiteUrl)); }
            catch (StoreProblem invalid) { state.setSiteUrl(""); }
        }
        return state;
    }

    private Status toStatus(SitemapJobState state) {
        Instant nextRun = CronExpression.parse(cron).next(ZonedDateTime.now(ZoneOffset.UTC)).toInstant();
        String publicUrl = state.getSiteUrl().isBlank() ? "" : state.getSiteUrl() + "/sitemap.xml";
        return new Status(state.getSiteUrl(), cron, "UTC", nextRun == null ? null : nextRun,
            state.getLastAttemptAt(), state.getLastSuccessAt(), state.getLastRunSucceeded(), state.getUrlCount(),
            state.getLastError(), publicUrl);
    }

    private Map<String, Instant> collectUrls() {
        Map<String, Instant> urls = new LinkedHashMap<>();
        for (ContentPage page : pages.findByPublishedTrueOrderByTitleAsc()) {
            String path = pagePath(page);
            if (path != null) urls.putIfAbsent(path, page.getUpdatedAt());
        }
        for (Product product : products.findByActiveTrueOrderByFeaturedDescNameAsc()) {
            urls.putIfAbsent("/products/" + product.getSlug(), null);
        }
        return urls;
    }

    private String pagePath(ContentPage page) {
        String template = page.getTemplateKey();
        if (template == null) return "/pages/" + page.getSlug();
        if (ROUTE_TEMPLATES.contains(template)) return "home".equals(template) ? "/" : "/shop";
        if (EXCLUDED_TEMPLATES.contains(template)) return null;
        return null;
    }

    private String toXml(String baseUrl, Map<String, Instant> urls) {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        urls.forEach((path, modified) -> {
            xml.append("  <url><loc>").append(escapeXml(baseUrl + path)).append("</loc>");
            if (modified != null) {
                xml.append("<lastmod>").append(DateTimeFormatter.ISO_LOCAL_DATE.format(modified.atZone(ZoneOffset.UTC))).append("</lastmod>");
            }
            xml.append("</url>\n");
        });
        return xml.append("</urlset>\n").toString();
    }

    private String normalizeSiteUrl(String value) {
        if (value == null || value.isBlank()) throw new StoreProblem(HttpStatus.BAD_REQUEST, "validSitemapSiteUrlRequired");
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                || (uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath()))) {
                throw new IllegalArgumentException("Enter the storefront origin, such as https://example.com, without a path.");
            }
            return uri.getScheme().toLowerCase() + "://" + uri.getRawAuthority();
        } catch (IllegalArgumentException invalid) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "validSitemapSiteUrlRequired");
        }
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }
}

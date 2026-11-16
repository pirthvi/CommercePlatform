package com.commerceplatform.config;

import com.commerceplatform.service.PageLayouts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.commerceplatform.api.StorefrontContentRequest;
import com.commerceplatform.api.StorefrontContentResponse;
import com.commerceplatform.domain.*;
import com.commerceplatform.repository.*;
import com.commerceplatform.service.CategoryService;
import com.commerceplatform.service.InventoryService;
import com.commerceplatform.service.StorefrontContentMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.*;

@Component
@Order(0)
public class SeedData implements ApplicationRunner {
    private final StorefrontSettingsRepository sites;
    private final CommerceSettingsRepository commerce;
    private final ContentPageRepository pages;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final CategoryService catalog;
    private final InventoryService inventory;
    private final StorefrontContentMapper content;
    private final InterfaceTextRepository texts;
    private final ObjectMapper json;
    private final PageLayouts layouts;

    public SeedData(StorefrontSettingsRepository sites, CommerceSettingsRepository commerce, ContentPageRepository pages,
                    ProductRepository products, CategoryRepository categories, CategoryService catalog, InventoryService inventory,
                    StorefrontContentMapper content, InterfaceTextRepository texts, ObjectMapper json, PageLayouts layouts) {
        this.sites = sites; this.commerce = commerce; this.pages = pages; this.products = products;
        this.categories = categories; this.catalog = catalog; this.inventory = inventory; this.content = content; this.texts = texts; this.json = json; this.layouts = layouts;
    }

    @Override @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        for (var product : products.findByCatalogCategoryIsNull()) {
            String name = product.getCategory();
            product.assignCategory(catalog.resolve(null, name == null || name.isBlank() ? "General" : name));
            inventory.recordOpeningBalance(product);
        }
        if (!commerce.existsById(1L)) {
            if (categories.count() == 0) catalog.create("General", "");
            initializeStore();
            commerce.save(new CommerceSettings());
        }
        var existingKeys = new HashSet<>(texts.findAll().stream().map(InterfaceText::getId).toList());
        resource("interface-text").fields().forEachRemaining(entry -> {
            if (!existingKeys.contains(entry.getKey())) texts.save(new InterfaceText(entry.getKey(), entry.getValue().asText()));
        });
        migratePageLayouts();
    }

    private void migratePageLayouts() throws IOException {
        var defaults = resource("defaults");
        for (var entry : defaults.withArray("pages")) {
            String slug = entry.path("slug").asText();
            if (!PageLayouts.SYSTEM_PAGES.contains(slug)) continue;
            var page = pages.findBySlug(slug).orElseGet(() -> pages.save(new ContentPage(
                entry.path("title").asText(), slug, entry.path("eyebrow").asText(), entry.path("body").asText(),
                entry.path("metaDescription").asText(), true)));
            if (page.getTemplateKey() == null) page.setTemplateKey(slug);
        }
        if (pages.findByTemplateKey("product").isEmpty()) {
            String slug = "product-template";
            for (int index = 2; pages.findBySlug(slug).isPresent(); index++) slug = "product-template-" + index;
            String title = texts.findById("studio.navigation.productPage").orElseThrow().getWording();
            var product = new ContentPage(title, slug, "", "", "", true);
            product.setTemplateKey("product");
            pages.save(product);
        }
        var home = json.createArrayNode();
        var site = sites.findById(1L);
        if (site.isPresent()) home = json.valueToTree(content.toResponse(site.get()).homeSections());
        for (var page : pages.findAll()) layouts.migrate(page, home);
    }

    private JsonNode resource(String name) throws IOException {
        try (var stream = new ClassPathResource("commerce/" + name + ".json").getInputStream()) { return json.readTree(stream); }
    }
    private void initializeStore() throws IOException {
        var defaults = resource("defaults");
        var legacy = resource("legacy-defaults");
        var existing = sites.findById(1L);
        StorefrontSettings store = existing.orElseGet(StorefrontSettings::create);
        ObjectNode base = canonicalSite(defaults.get("site"));
        if (existing.isPresent()) {
            ObjectNode current = json.valueToTree(content.toResponse(store));
            var previous = canonicalSite(legacy.get("site"));
            for (JsonNode section : current.withArray("homeSections")) {
                for (int index = 0; index < previous.withArray("homeSections").size(); index++) {
                    JsonNode old = previous.get("homeSections").get(index);
                    boolean sameSection = section.path("imageKey").isTextual()
                        && !section.path("imageKey").asText().isBlank()
                        ? section.path("imageKey").equals(old.path("imageKey")) : section.path("type").equals(old.path("type"));
                    if (sameSection) {
                        replaceDefaults((ObjectNode) section, old, base.get("homeSections").get(index), Set.of("type","visible","imageKey"));
                        break;
                    }
                }
            }
            replaceDefaults(current, previous, base, Set.of("homeSections","logoUrl"));
            base = current;
        }
        normalizeCategoryLinks(base);
        var request = json.treeToValue(base, StorefrontContentRequest.class);
        content.apply(store, request, request.homeSections());
        sites.save(store);

        for (JsonNode next : defaults.get("pages")) {
            String slug = next.path("slug").asText();
            var persisted = pages.findBySlug(slug);
            if (persisted.isPresent()) {
                var page = persisted.get();
                JsonNode previous = legacy.get("pages").findParents("slug").stream().filter(p -> slug.equals(p.path("slug").asText())).findFirst().orElse(null);
                if (previous == null) continue;
                ObjectNode current = json.valueToTree(page);
                replaceDefaults(current, previous, next, Set.of("id","slug","published","updatedAt"));
                page.setTitle(current.path("title").asText()); page.setBody(current.path("body").asText());
                page.setEyebrow(current.path("eyebrow").asText(null)); page.setMetaDescription(current.path("metaDescription").asText(null));
                page.setButtonLabel(current.path("buttonLabel").asText(null)); page.setButtonLink(current.path("buttonLink").asText(null));
                page.setHeroImageUrl(current.path("heroImageUrl").asText(null));
            } else if (existing.isEmpty()) {
                pages.save(new ContentPage(next.path("title").asText(),slug,next.path("eyebrow").asText(),next.path("body").asText(),next.path("metaDescription").asText(),true));
            }
        }
    }
    private ObjectNode canonicalSite(JsonNode node) {
        return json.valueToTree(json.convertValue(node, StorefrontContentResponse.class));
    }
    private void replaceDefaults(ObjectNode current, JsonNode old, JsonNode next, Set<String> excluded) {
        old.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            if (!excluded.contains(key) && Objects.equals(current.path(key), entry.getValue())) {
                if (next.has(key)) current.set(key, next.get(key)); else current.remove(key);
            }
        });
    }
    private void normalizeCategoryLinks(ObjectNode site) {
        var slugs = new HashMap<String,String>();
        categories.findAll().forEach(c -> slugs.put(c.getNormalizedName(), c.getSlug()));
        for (String key : List.of("category","buttonCategory")) {
            for (JsonNode node : site.findParents(key)) {
                String slug = slugs.get(node.path(key).asText("").toLowerCase(Locale.ROOT));
                if (slug != null) ((ObjectNode) node).put(key, slug);
            }
        }
    }
}

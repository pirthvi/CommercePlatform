package com.commerceplatform.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.commerceplatform.api.StorefrontContentResponse;
import com.commerceplatform.domain.ContentPage;
import com.commerceplatform.domain.StorefrontSettings;
import com.commerceplatform.repository.ContentPageRepository;
import com.commerceplatform.repository.InterfaceTextRepository;
import com.commerceplatform.repository.StorefrontSettingsRepository;
import com.commerceplatform.service.PageLayouts;
import com.commerceplatform.service.StorefrontContentMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@Order(1)
class ApparelStarter implements ApplicationRunner {
    private static final int VERSION = 3;
    private static final Set<String> PHOTO_BLOCKS = Set.of("text", "hero", "banner", "story", "editorial",
        "catalog", "cart", "checkout", "customer-form", "account", "receipt");
    private final StorefrontSettingsRepository sites;
    private final ContentPageRepository pages;
    private final StorefrontContentMapper content;
    private final PageLayouts layouts;
    private final ApparelCatalog catalog;
    private final InterfaceTextRepository texts;
    private final ObjectMapper json;

    ApparelStarter(StorefrontSettingsRepository sites, ContentPageRepository pages, StorefrontContentMapper content,
                   PageLayouts layouts, ApparelCatalog catalog, InterfaceTextRepository texts, ObjectMapper json) {
        this.sites = sites;
        this.pages = pages;
        this.content = content;
        this.layouts = layouts;
        this.catalog = catalog;
        this.texts = texts;
        this.json = json;
    }

    @Override
    @Transactional(rollbackFor = IOException.class)
    public void run(ApplicationArguments args) throws IOException {
        var site = sites.findForEditing().orElseThrow();
        if (site.hasApparelStarter(VERSION)) return;
        var starter = resource("starter-pages");
        var generic = resource("defaults");
        var legacy = resource("legacy-defaults");
        var selection = catalog.install();
        resolveCategories(starter, selection);
        applyBrand(site, starter.path("site"), generic.path("site"), legacy.path("site"));
        for (var entry : starter.path("wording").properties()) {
            var wording = entry.getValue();
            texts.findById(entry.getKey()).filter(row -> row.getWording().equals(wording.get(0).asText()))
                .ifPresent(row -> row.setWording(wording.get(1).asText()));
        }
        // Information pages may be missing from an older installation that already has store settings.
        for (var original : generic.path("pages")) {
            String slug = original.path("slug").asText();
            if (!PageLayouts.SYSTEM_PAGES.contains(slug) && pages.findBySlug(slug).isEmpty()) {
                var page = pages.save(new ContentPage(original.path("title").asText(), slug,
                    original.path("eyebrow").asText(), original.path("body").asText(), original.path("metaDescription").asText(), true));
                layouts.migrate(page, json.createArrayNode());
            }
        }
        for (var page : pages.findAll()) {
            String key = page.getTemplateKey() == null ? page.getSlug() : page.getTemplateKey();
            var spec = starter.path("pages").path(key);
            var current = layouts.read(page);
            if (!spec.isMissingNode() && page.getLayoutRevision() == 1 && isInitialLayout(key, current)) {
                var original = by(generic.path("pages"), "slug", key);
                var old = by(legacy.path("pages"), "slug", key);
                String title = "home".equals(key) ? site.getBrandName() : spec.path("title").asText();
                page.setTitle(defaultText(page.getTitle(), original, old, "title", title));
                page.setBody(defaultText(page.getBody(), original, old, "body", spec.path("body").asText()));
                page.setMetaDescription(defaultText(page.getMetaDescription(), original, old, "metaDescription", spec.path("body").asText()));
                var composed = compose(spec, selection);
                if ("home".equals(key)) {
                    preserveHomeEdits(current, composed, generic.path("site").path("homeSections"), legacy.path("site").path("homeSections"));
                } else if (!"product".equals(key)) {
                    var main = (ObjectNode) composed.get(0);
                    main.put("heading", page.getTitle()).put("body", page.getBody());
                    String eyebrow = defaultText(page.getEyebrow(), original, old, "eyebrow", "");
                    main.put("eyebrow", eyebrow);
                    if (!current.isEmpty()) {
                        for (var saved : current) {
                            if (requiredMain(key).equals(saved.path("type").asText()) && saved.path("imageUrl").asText("").startsWith("/api/")) {
                                main.set("imageUrl", saved.get("imageUrl"));
                                main.put("imageAlt", saved.path("imageAlt").asText(page.getTitle()));
                                break;
                            }
                        }
                        for (var block : current) if ("button".equals(block.path("type").asText())) composed.add(block);
                    }
                }
                saveLayout(site, page, composed);
            } else if (supplyMissingPhoto(current, spec, starter.path("customPageImage"))) {
                saveLayout(site, page, current);
            }
            current = layouts.read(page);
            if (layouts.ensureSiteFrame(current)) saveLayout(site, page, current);
        }
        site.recordApparelStarter(VERSION);
    }

    private ArrayNode compose(JsonNode spec, ApparelCatalog.Selection selection) throws IOException {
        var result = layouts.instantiate((ArrayNode) spec.get("blocks"));
        for (var value : result) {
            var block = (ObjectNode) value;
            var photo = block.remove("photo");
            if (photo != null) block.put("imageUrl", catalog.pageImage(photo.asText()));
            if (block.remove("selection") != null) block.set("productIds", json.valueToTree(selection.productIds()));
        }
        return result;
    }

    private boolean isInitialLayout(String key, ArrayNode sections) {
        if (sections.isEmpty()) return true;
        var types = sections.findValuesAsText("type").stream().filter(type -> !Set.of("header", "footer").contains(type)).toList();
        if ("home".equals(key)) {
            return types.equals(List.of("hero","benefits","collections","featured","story","values","banner"))
                || types.equals(List.of("announcement","hero","benefits","collections","product-grid","story","banner"));
        }
        if ("product".equals(key)) {
            return types.equals(List.of("product-gallery","product-purchase","related-products"));
        }
        var required = PageLayouts.required(key);
        String main = required.isEmpty() ? "text" : required.get(0);
        return !types.isEmpty() && main.equals(types.get(0)) && (types.size() == 1 ||
            (types.size() == 2 && "button".equals(types.get(1))));
    }

    private String requiredMain(String key) {
        var required = PageLayouts.required(key);
        return required.isEmpty() ? "text" : required.get(0);
    }

    private void preserveHomeEdits(ArrayNode previous, ArrayNode next, JsonNode defaults, JsonNode legacy) {
        for (var old : previous) {
            String type = old.path("type").asText();
            if (Set.of("header", "footer").contains(type)) continue;
            var original = by(defaults, "type", type);
            var previousDefault = by(legacy, "type", type);
            var target = by(next, "type", "featured".equals(type) ? "product-grid" : type);
            if (target.isMissingNode()) {
                if (old.path("visible").asBoolean() && !factoryItems(old, original, previousDefault)) next.add(old);
                continue;
            }
            var block = (ObjectNode) target;
            for (var entry : old.properties()) {
                String field = entry.getKey();
                if (!entry.getValue().isTextual() || Set.of("id","type","imageKey").contains(field)) continue;
                String value = entry.getValue().textValue();
                if (!value.isBlank() && !value.equals(original.path(field).asText()) && !value.equals(previousDefault.path(field).asText())) {
                    if (!"imageUrl".equals(field) || value.startsWith("/api/")) block.put(field, value);
                }
            }
            if (type.equals(block.path("type").asText()) && !factoryItems(old, original, previousDefault)) {
                block.set("items", old.path("items"));
            }
        }
    }

    private boolean factoryItems(JsonNode block, JsonNode original, JsonNode legacy) {
        // Compare the original content, independent of generated item IDs and omitted optional fields.
        var items = json.convertValue(block, StorefrontContentResponse.HomeSection.class).items();
        return items.equals(json.convertValue(original, StorefrontContentResponse.HomeSection.class).items())
            || items.equals(json.convertValue(legacy, StorefrontContentResponse.HomeSection.class).items());
    }

    private boolean supplyMissingPhoto(ArrayNode blocks, JsonNode spec, JsonNode fallback) throws IOException {
        for (var block : blocks) {
            if (block.path("visible").asBoolean() && ("product-gallery".equals(block.path("type").asText())
                || block.findValues("imageUrl").stream().anyMatch(url -> url.isTextual() && !url.textValue().isBlank()))) return false;
        }
        JsonNode source = fallback;
        for (var definition : spec.path("blocks")) if (definition.has("photo")) { source = definition; break; }
        ObjectNode target = null;
        for (var block : blocks) {
            if (block.path("visible").asBoolean() && PHOTO_BLOCKS.contains(block.path("type").asText())) {
                target = (ObjectNode) block;
                break;
            }
        }
        if (target == null) {
            var definition = json.createArrayNode();
            definition.addObject().put("type", "text");
            target = (ObjectNode) layouts.instantiate(definition).get(0);
            blocks.add(target);
        }
        target.put("imageUrl", catalog.pageImage(source.path("photo").asText()));
        target.put("imageAlt", source.path("imageAlt").asText());
        return true;
    }

    private void saveLayout(StorefrontSettings site, ContentPage page, ArrayNode blocks) {
        layouts.replace(page, blocks);
        if ("home".equals(page.getTemplateKey())) site.setHomeSectionsJson(layouts.legacyHomeSections(blocks).toString());
        else if (!"product".equals(page.getTemplateKey())) layouts.updateLegacyFields(page, blocks);
    }

    private void applyBrand(StorefrontSettings site, JsonNode spec, JsonNode defaults, JsonNode legacy) {
        var before = content.toResponse(site);
        site.setBrandName(defaultText(before.brandName(), defaults, legacy, "brandName", spec.path("brandName").asText()));
        site.setBrandMark(defaultText(before.brandMark(), defaults, legacy, "brandMark", spec.path("brandMark").asText()));
        site.setBrandTagline(defaultText(before.brandTagline(), defaults, legacy, "brandTagline", spec.path("brandTagline").asText()));
        site.setAnnouncementText(defaultText(before.announcementText(), defaults, legacy, "announcementText", spec.path("announcementText").asText()));
        site.setAnnouncementSecondary(defaultText(before.announcementSecondary(), defaults, legacy, "announcementSecondary", spec.path("announcementSecondary").asText()));
        site.setFooterStory(defaultText(before.footerStory(), defaults, legacy, "footerStory", spec.path("footerStory").asText()));
        var neutral = json.convertValue(defaults, StorefrontContentResponse.class);
        var old = json.convertValue(legacy, StorefrontContentResponse.class);
        var apparel = json.convertValue(spec, StorefrontContentResponse.class);
        if (before.navigation().equals(neutral.navigation()) || before.navigation().equals(old.navigation())) content.applyNavigation(site, apparel.navigation());
        if (before.footerColumns().equals(neutral.footerColumns()) || before.footerColumns().equals(old.footerColumns())) {
            site.setFooterColumnsJson(spec.path("footerColumns").toString());
        }
    }

    private String defaultText(String current, JsonNode original, JsonNode old, String field, String next) {
        if (current == null || current.isBlank() || Objects.equals(current, original.path(field).asText())
            || Objects.equals(current, old.path(field).asText())) return next;
        return current;
    }

    private void resolveCategories(JsonNode starter, ApparelCatalog.Selection selection) {
        for (String field : List.of("category", "buttonCategory")) {
            for (var owner : starter.findParents(field)) {
                String slug = selection.categorySlugs().get(owner.path(field).asText());
                if (slug != null) ((ObjectNode) owner).put(field, slug);
            }
        }
    }

    private JsonNode by(JsonNode entries, String key, String value) {
        for (var entry : entries) if (value.equals(entry.path(key).asText())) return entry;
        return json.missingNode();
    }

    private JsonNode resource(String name) throws IOException {
        try (var stream = new ClassPathResource("commerce/" + name + ".json").getInputStream()) {
            return json.readTree(stream);
        }
    }
}

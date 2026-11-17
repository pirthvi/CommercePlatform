package com.commerceplatform.service;

import java.util.HashSet;
import java.util.Objects;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.CategoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.commerceplatform.api.StoreProblem;
import com.commerceplatform.domain.ContentPage;
import com.commerceplatform.repository.ContentPageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PageLayouts {
    public static final List<String> SYSTEM_PAGES = List.of("home", "shop", "cart", "checkout", "login", "register", "account", "order-success");
    public static final Set<String> CONTENT_TYPES = Set.of("announcement", "hero", "benefits", "collections", "featured", "story", "values",
        "banner", "editorial", "text", "gallery", "button", "faq", "product-grid");
    private final ObjectMapper json;
    private final ContentPageRepository pages;
    private final CategoryRepository categories;
    private final ProductRepository products;

    public PageLayouts(ObjectMapper json, ContentPageRepository pages, CategoryRepository categories,
                       ProductRepository products) {
        this.categories = categories;
        this.products = products;
        this.json = json;
        this.pages = pages;
    }

    public static List<String> required(String template) {
        if (template == null) return List.of();
        return switch (template) {
            case "shop" -> List.of("catalog");
            case "product" -> List.of("product-gallery", "product-purchase");
            case "cart", "checkout", "account" -> List.of(template);
            case "register", "login" -> List.of("customer-form");
            case "order-success" -> List.of("receipt");
            default -> List.of();
        };
    }

    public ArrayNode read(ContentPage page) {
        try {
            return (ArrayNode) json.readTree(page.getLayoutJson());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot read saved page components", e);
        }
    }

    public void replace(ContentPage page, ArrayNode sections) {
        page.replaceLayout(sections.toString());
    }

    public void migrate(ContentPage page, ArrayNode homeSections) {
        if (page.getLayoutJson() != null) return;
        String template = page.getTemplateKey();
        ArrayNode sections = json.createArrayNode();
        if ("home".equals(template)) {
            sections = instantiate(homeSections);
        } else if ("product".equals(template)) {
            for (String kind : List.of("product-gallery", "product-purchase", "related-products")) add(sections, kind);
        } else {
            var required = required(template);
            var main = add(sections, required.isEmpty() ? "text" : required.get(0));
            main.put("heading", page.getTitle());
            main.put("eyebrow", page.getEyebrow());
            main.put("body", page.getBody());
            if (page.getHeroImageUrl() != null) {
                main.put("imageUrl", page.getHeroImageUrl());
                main.put("imageAlt", page.getTitle());
            }
            if (page.getButtonLabel() != null && page.getButtonLink() != null) {
                var button = add(sections, "button");
                button.put("buttonLabel", page.getButtonLabel());
                button.put("buttonPath", page.getButtonLink());
            }
        }
        replace(page, sections);
    }

    public boolean referencesCategory(long id, String slug) {
        return pages.findComponentLayouts().stream().anyMatch(value -> {
            try {
                var layout = json.readTree(value);
                return layout.findValues("categoryId").stream().anyMatch(node -> node.isNumber() && node.asLong() == id)
                    || layout.findValuesAsText("buttonCategory").contains(slug) || layout.findValuesAsText("category").contains(slug);
            } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read category references", ex); }
        });
    }

    public void checkSelections(ArrayNode sections, ArrayNode previous) {
        Set<Long> productIds = new HashSet<>(), categoryIds = new HashSet<>();
        Set<String> slugs = new HashSet<>();
        for (var section : sections) {
            var selected = section.path("productIds");
            for (var id : selected) productIds.add(id.longValue());
            if (section.path("categoryId").isIntegralNumber()) categoryIds.add(section.get("categoryId").longValue());
        }
        slugs.addAll(categorySlugs(sections));
        // Previously stored selections may refer to retired products or historical category names.
        if (previous != null) {
            for (var section : previous) {
                for (var id : section.path("productIds")) productIds.remove(id.longValue());
                if (section.path("categoryId").isIntegralNumber()) categoryIds.remove(section.get("categoryId").longValue());
            }
            slugs.removeAll(categorySlugs(previous));
        }
        if ((!productIds.isEmpty() && products.findAllById(productIds).size() != productIds.size())
            || (!categoryIds.isEmpty() && categories.findAllById(categoryIds).size() != categoryIds.size())
            || slugs.stream().anyMatch(slug -> !categories.existsBySlug(slug))) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "invalidLayout");
        }
    }

    private Set<String> categorySlugs(ArrayNode sections) {
        Set<String> slugs = new HashSet<>();
        for (String field : List.of("buttonCategory", "category")) {
            sections.findValues(field).stream().filter(JsonNode::isTextual).map(JsonNode::textValue)
                .filter(value -> !value.isBlank()).forEach(slugs::add);
        }
        return slugs;
    }

    public void updateLegacyFields(ContentPage page, ArrayNode sections) {
        var required = required(page.getTemplateKey());
        String kind = required.isEmpty() ? "text" : required.get(0);
        JsonNode main = null, button = null;
        for (var section : sections) {
            if (!section.path("visible").asBoolean()) continue;
            if (main == null && kind.equals(section.path("type").asText())) main = section;
            if (button == null && "button".equals(section.path("type").asText())) button = section;
        }
        page.setEyebrow(main == null ? null : limited(main, "eyebrow", 140));
        page.setBody(main == null ? "" : main.path("body").asText(""));
        page.setHeroImageUrl(main == null ? null : limited(main, "imageUrl", 700));
        page.setButtonLabel(button == null ? null : limited(button, "buttonLabel", 100));
        page.setButtonLink(button == null ? null : limited(button, "buttonPath", 240));
    }

    private String limited(JsonNode node, String key, int limit) {
        var value = node.path(key).asText(null);
        return value == null || value.length() <= limit ? value : value.substring(0, limit);
    }

    public ArrayNode instantiate(ArrayNode definitions) {
        ArrayNode sections = json.createArrayNode();
        for (var value : definitions) {
            var section = (ObjectNode) value.deepCopy();
            section.put("id", UUID.randomUUID().toString());
            if (!section.has("visible")) section.put("visible", true);
            if (!section.hasNonNull("items")) section.putArray("items");
            for (var item : section.withArray("items")) ((ObjectNode) item).put("id", UUID.randomUUID().toString());
            sections.add(section);
        }
        return sections;
    }

    // A legacy page edit updates the corresponding blocks without discarding the rest of the composition.
    public void updateLegacy(ContentPage page, String previousImage, String previousLabel, String previousLink) {
        if (page.getLayoutJson() == null) { migrate(page, json.createArrayNode()); return; }
        if ("home".equals(page.getTemplateKey()) || "product".equals(page.getTemplateKey())) return;
        var sections = read(page);
        var required = required(page.getTemplateKey());
        String kind = required.isEmpty() ? "text" : required.get(0);
        ObjectNode main = null, button = null;
        for (var value : sections) {
            var section = (ObjectNode) value;
            if (main == null && kind.equals(section.path("type").asText())) main = section;
            if (button == null && "button".equals(section.path("type").asText())
                && Objects.equals(previousLink, section.path("buttonPath").asText(null))
                && Objects.equals(previousLabel, section.path("buttonLabel").asText(null))) button = section;
        }
        if (main == null) main = add(sections, kind);
        main.put("heading", page.getTitle()).put("eyebrow", page.getEyebrow()).put("body", page.getBody());
        if (!Objects.equals(previousImage, page.getHeroImageUrl())) main.put("imageUrl", page.getHeroImageUrl());
        if (page.getButtonLabel() != null && !page.getButtonLabel().isBlank() && page.getButtonLink() != null && !page.getButtonLink().isBlank()) {
            if (button == null) button = add(sections, "button");
            button.put("buttonLabel", page.getButtonLabel()).put("buttonPath", page.getButtonLink());
        } else if (button != null) {
            for (int i = 0; i < sections.size(); i++) if (sections.get(i) == button) { sections.remove(i); break; }
        }
        replace(page, sections);
    }

    public void updateLegacyHome(List<?> home) {
        pages.findByTemplateKey("home").ifPresent(page -> {
            var locked = pages.findForEditing(page.getId()).orElseThrow();
            // An older editor only understands the original home section types.
            var sections = read(locked);
            for (var section : sections) {
                if (!Set.of("announcement","hero","benefits","collections","featured","story","values","banner","editorial").contains(section.path("type").asText())) {
                    throw new StoreProblem(HttpStatus.CONFLICT, "usePageBuilder");
                }
            }
            replace(locked, instantiate(json.valueToTree(home)));
        });
    }

    private ObjectNode add(ArrayNode sections, String kind) {
        var section = sections.addObject();
        section.put("id", UUID.randomUUID().toString()).put("type", kind).put("visible", true).putArray("items");
        return section;
    }
}

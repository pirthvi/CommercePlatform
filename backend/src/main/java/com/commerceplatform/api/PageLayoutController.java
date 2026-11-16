package com.commerceplatform.api;

import com.commerceplatform.repository.StorefrontSettingsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.commerceplatform.domain.ContentPage;
import com.commerceplatform.domain.StorefrontContentAsset;
import com.commerceplatform.repository.ContentPageRepository;
import com.commerceplatform.repository.StorefrontContentAssetRepository;
import com.commerceplatform.service.PageLayouts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class PageLayoutController {
    private final ContentPageRepository pages;
    private final StorefrontContentAssetRepository assets;
    private final PageLayouts layouts;
    private final StorefrontSettingsRepository settings;
    private static final String IMAGE_PATH = "/api/content/site/assets/";
    private static final Set<String> COPY = Set.of("eyebrow", "heading", "body", "imageAlt", "imageCaption", "buttonLabel",
        "buttonPath", "buttonCategory", "linkLabel", "linkPath", "cardEyebrow", "cardLinkLabel", "emptyMessage", "emptyLinkLabel");

    private static final Set<String> STRUCTURE = Set.of("type", "id", "visible", "imageKey", "clearImage", "imageUrl", "items", "productIds", "categoryId");
    private static final Set<String> ITEM_FIELDS = Set.of("id", "icon", "title", "body", "category", "path", "imageAlt", "imageUrl");

    public PageLayoutController(ContentPageRepository pages, StorefrontContentAssetRepository assets, PageLayouts layouts,
                                StorefrontSettingsRepository settings) {
        this.settings = settings;
        this.pages = pages; this.assets = assets; this.layouts = layouts;
    }

    public record Layout(Long id, String templateKey, String title, String slug, String metaDescription,
                         boolean published, int revision, List<String> required, List<JsonNode> sections) {}
    public record Edit(@NotBlank @Size(max=140) String title,
                       @NotBlank @Size(max=120) @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
                       @Size(max=240) String metaDescription, @NotNull Boolean published,
                       @NotNull @PositiveOrZero Integer revision, @NotNull ArrayNode sections) {}

    @GetMapping("/content/layouts/{kind}/{key}")
    public Layout published(@PathVariable String kind, @PathVariable String key) {
        var page = switch (kind) {
            case "page" -> pages.findBySlugAndPublishedTrue(key);
            case "template" -> pages.findByTemplateKey(key).filter(ContentPage::isPublished);
            default -> Optional.<ContentPage>empty();
        };
        return view(page.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), true);
    }

    @GetMapping("/admin/page-layouts/{id}")
    public Layout edit(@PathVariable Long id) {
        return view(pages.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), false);
    }
    @GetMapping("/admin/page-layouts/template/{key}")
    public Layout template(@PathVariable String key) {
        return view(pages.findByTemplateKey(key).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), false);
    }

    @Transactional @PostMapping(value="/admin/page-layouts", consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public Layout create(@Valid @RequestPart("layout") Edit edit, MultipartHttpServletRequest request) {
        settings.findForEditing().orElseThrow();
        var page = pages.save(new ContentPage(edit.title(), edit.slug(), "", "", edit.metaDescription(), edit.published()));
        return write(page, edit, request);
    }

    @Transactional @PutMapping(value="/admin/page-layouts/{id}", consumes="multipart/form-data")
    public Layout save(@PathVariable Long id, @Valid @RequestPart("layout") Edit edit, MultipartHttpServletRequest request) {
        // Serialize category-reference checks with category deletion and legacy home editing.
        settings.findForEditing().orElseThrow();
        var page = pages.findForEditing(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return write(page, edit, request);
    }

    private Layout write(ContentPage page, Edit edit, MultipartHttpServletRequest request) {
        if (page.getLayoutRevision() != edit.revision()) throw new StoreProblem(HttpStatus.CONFLICT, "layoutConflict");
        if (page.getTemplateKey() != null && !page.getSlug().equals(edit.slug())) throw invalid();
        var previousImages = new HashSet<String>();
        var previous = page.getLayoutJson() == null ? null : layouts.read(page);
        if (previous != null) collectImages(previous, previousImages);
        var pending = new HashMap<String, MultipartFile>();
        request.getMultiFileMap().forEach((name, parts) -> {
            if (name.equals("layout")) return;
            if (!name.startsWith("image_") || parts.size() != 1) throw invalid();
            pending.put(name.substring(6), parts.get(0));
        });

        var sections = edit.sections();
        var required = PageLayouts.required(page.getTemplateKey());
        Set<String> ids = new HashSet<>(), functional = new HashSet<>();
        if (sections.size() > 80 || sections.toString().length() > 512000) throw invalid();
        for (var node : sections) {
            if (!(node instanceof ObjectNode section)) throw invalid();
            section.fieldNames().forEachRemaining(key -> { if (!STRUCTURE.contains(key) && !COPY.contains(key)) throw invalid(); });
            String kind = section.path("type").asText();
            if (!section.path("visible").isBoolean()) throw invalid();
            if (!PageLayouts.CONTENT_TYPES.contains(kind)) {
                boolean optional = "product".equals(page.getTemplateKey()) && kind.equals("related-products");
                if ((!required.contains(kind) && !optional) || !functional.add(kind) || (!optional && !section.path("visible").asBoolean())) throw invalid();
            }
            for (String field : COPY) string(section, field, field.equals("body") ? 12000 : 1000);
            link(section, "buttonPath"); link(section, "linkPath");
            image(page, section, ids, previousImages, pending);
            if (!(section.get("items") instanceof ArrayNode items) || items.size() > 80) throw invalid();
            for (var item : items) {
                if (!(item instanceof ObjectNode row)) throw invalid();
                row.fieldNames().forEachRemaining(key -> { if (!ITEM_FIELDS.contains(key)) throw invalid(); });
                for (String field : List.of("icon","title","body","category","path","imageAlt")) string(row, field, field.equals("body") ? 12000 : 1000);
                if (!kind.equals("featured")) link(row, "path");
                image(page, row, ids, previousImages, pending);
            }
            if (!kind.equals("product-grid") && (section.hasNonNull("productIds") || section.hasNonNull("categoryId"))) throw invalid();
            var productIds = section.get("productIds");
            if (productIds != null && (!productIds.isArray() || productIds.size() > 100)) throw invalid();
            if (productIds != null) for (var id : productIds) if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() < 1) throw invalid();
            var category = section.get("categoryId");
            if (category != null && !category.isNull() && (!category.isIntegralNumber() || !category.canConvertToLong() || category.asLong() < 1)) throw invalid();
        }
        if (!functional.containsAll(required) || !pending.isEmpty()) throw invalid();
        layouts.checkSelections(sections, previous);
        page.setTitle(edit.title()); page.setSlug(edit.slug()); page.setMetaDescription(edit.metaDescription()); page.setPublished(edit.published());
        layouts.replace(page, sections);
        if ("home".equals(page.getTemplateKey())) settings.findById(1L).orElseThrow().setHomeSectionsJson(sections.toString());
        else layouts.updateLegacyFields(page, sections);
        // Retire only blobs owned by this page and removed from its published composition.
        var retained = new HashSet<String>();
        collectImages(sections, retained);
        previousImages.removeAll(retained);
        String prefix = IMAGE_PATH + "layout." + page.getId() + ".";
        for (String url : previousImages) if (url.startsWith(prefix)) assets.deleteById(url.substring(IMAGE_PATH.length()));
        return view(page, false);
    }

    private void image(ContentPage page, ObjectNode owner, Set<String> ids, Set<String> previous, Map<String, MultipartFile> pending) {
        String id = owner.path("id").asText("");
        if (!id.matches("[A-Za-z0-9_-]{1,64}") || !ids.add(id)) throw invalid();
        string(owner, "imageUrl", 4000);
        MultipartFile upload = pending.remove(id);
        if (upload == null) {
            String url = owner.path("imageUrl").asText("");
            if (!url.isEmpty() && !previous.contains(url)) throw invalid();
            return;
        }
        if (upload.getSize() > 8L * 1024 * 1024) throw new StoreProblem(HttpStatus.PAYLOAD_TOO_LARGE, "eachHomePageImageMustBe8Mb");
        try {
            byte[] bytes = upload.getBytes();
            String type = ImageContentType.detect(bytes);
            if (type == null) throw new StoreProblem(HttpStatus.BAD_REQUEST, "uploadAValidJpegPngOrWebpImage");
            String key = "layout." + page.getId() + "." + UUID.randomUUID();
            assets.save(new StorefrontContentAsset(key, type, bytes));
            owner.put("imageUrl", IMAGE_PATH + key);
        } catch (IOException failed) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "theImageCouldNotBeRead");
        }
    }

    private void collectImages(ArrayNode sections, Set<String> urls) {
        sections.findValues("imageUrl").stream().filter(JsonNode::isTextual).map(JsonNode::textValue).forEach(urls::add);
    }
    private void string(ObjectNode node, String field, int max) {
        var v = node.get(field);
        if (v != null && !v.isNull() && (!v.isTextual() || v.textValue().length() > max)) throw invalid();
    }
    private void link(ObjectNode node, String field) {
        String link = node.path(field).asText("");
        if (!link.isBlank() && (link.startsWith("//") || link.contains("\\") || link.chars().anyMatch(Character::isISOControl)
            || !(link.startsWith("/") || link.startsWith("#") || link.matches("(?i)(https?://|mailto:|tel:).+")))) throw invalid();
    }

    private Layout view(ContentPage page, boolean publicView) {
        List<JsonNode> visible = new ArrayList<>();
        for (var section : layouts.read(page)) if (!publicView || section.path("visible").asBoolean()) visible.add(section);
        return new Layout(page.getId(), page.getTemplateKey(), page.getTitle(), page.getSlug(), page.getMetaDescription(),
            page.isPublished(), page.getLayoutRevision(), PageLayouts.required(page.getTemplateKey()), visible);
    }
    private StoreProblem invalid() { return new StoreProblem(HttpStatus.BAD_REQUEST, "invalidLayout"); }
}

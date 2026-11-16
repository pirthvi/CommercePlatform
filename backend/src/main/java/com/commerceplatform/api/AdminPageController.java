package com.commerceplatform.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.commerceplatform.repository.StorefrontContentAssetRepository;
import com.commerceplatform.service.PageLayouts;
import com.commerceplatform.domain.ContentPage;
import com.commerceplatform.domain.ContentPageImage;
import com.commerceplatform.repository.ContentPageImageRepository;
import com.commerceplatform.repository.ContentPageRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/admin/pages")
@Transactional
public class AdminPageController {
    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;

    private final ContentPageRepository pages;
    private final ContentPageImageRepository images;

    private final PageLayouts layouts;
    private final StorefrontContentAssetRepository assets;

    public AdminPageController(ContentPageRepository pages, ContentPageImageRepository images, PageLayouts layouts, StorefrontContentAssetRepository assets) {
        this.assets = assets;
        this.layouts = layouts;
        this.pages = pages;
        this.images = images;
    }

    @GetMapping
    public List<ContentPage> list() { return pages.findAll(); }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentPage create(
        @Valid @RequestPart("page") PageRequest body,
        @RequestPart(value = "heroImage", required = false) MultipartFile heroImage
    ) {
        UploadedImage upload = prepare(heroImage);
        ContentPage page = pages.save(toPage(body));
        if (upload != null) saveHeroImage(page, upload);
        layouts.migrate(page, JsonNodeFactory.instance.arrayNode());
        return pages.save(page);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ContentPage update(
        @PathVariable Long id,
        @Valid @RequestPart("page") PageRequest body,
        @RequestPart(value = "heroImage", required = false) MultipartFile heroImage
    ) {
        ContentPage page = pages.findForEditing(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (page.getTemplateKey() != null && !page.getSlug().equals(body.slug())) throw new StoreProblem(HttpStatus.BAD_REQUEST, "invalidInput");
        String previousImage = page.getHeroImageUrl(), previousLabel = page.getButtonLabel(), previousLink = page.getButtonLink();
        UploadedImage upload = prepare(heroImage);
        page.setTitle(body.title());
        page.setSlug(body.slug());
        page.setEyebrow(body.eyebrow());
        page.setBody(body.body());
        page.setMetaDescription(body.metaDescription());
        page.setPublished(body.published());
        page.setButtonLabel(body.buttonLabel());
        page.setButtonLink(body.buttonLink());

        if (upload != null) {
            saveHeroImage(page, upload);
        } else if (body.clearHeroImage()) {
            page.setHeroImageUrl(null);
        }
        layouts.updateLegacy(page, previousImage, previousLabel, previousLink);
        return pages.save(page);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        var page = pages.findForEditing(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (page.getTemplateKey() != null) throw new StoreProblem(HttpStatus.CONFLICT, "requiredStorePage");
        images.deleteByPageId(id);
        assets.deleteByPrefix("layout." + id + ".");
        pages.deleteById(id);
    }

    private ContentPage toPage(PageRequest body) {
        return new ContentPage(body.title(), body.slug(), body.eyebrow(), body.body(), body.metaDescription(),
            body.published(), null, body.buttonLabel(), body.buttonLink());
    }

    private void saveHeroImage(ContentPage page, UploadedImage upload) {
        ContentPageImage stored = images.save(new ContentPageImage(page.getId(), upload.contentType(), upload.data()));
        page.setHeroImageUrl("/api/content/pages/" + page.getId() + "/images/" + stored.getId());
    }

    private UploadedImage prepare(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new StoreProblem(HttpStatus.PAYLOAD_TOO_LARGE, "theHeroImageMustBe8MbOr");
        }
        try {
            byte[] data = file.getBytes();
            String contentType = detectImageType(data);
            if (contentType == null) {
                throw new StoreProblem(HttpStatus.BAD_REQUEST, "uploadAValidJpegPngOrWebpImage");
            }
            return new UploadedImage(contentType, data);
        } catch (IOException exception) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "theHeroImageCouldNotBeRead");
        }
    }

    private String detectImageType(byte[] data) {
        if (data.length >= 3 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8 && (data[2] & 0xff) == 0xff) return "image/jpeg";
        if (data.length >= 8 && (data[0] & 0xff) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G'
            && data[4] == 0x0d && data[5] == 0x0a && data[6] == 0x1a && data[7] == 0x0a) return "image/png";
        if (data.length >= 12 && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
            && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
        return null;
    }

    private record UploadedImage(String contentType, byte[] data) {}
}

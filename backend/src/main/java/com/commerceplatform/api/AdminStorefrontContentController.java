package com.commerceplatform.api;

import com.commerceplatform.service.PageLayouts;
import com.commerceplatform.domain.StorefrontSettings;
import com.commerceplatform.domain.StorefrontContentAsset;
import com.commerceplatform.repository.StorefrontContentAssetRepository;
import com.commerceplatform.repository.StorefrontSettingsRepository;
import com.commerceplatform.service.StorefrontContentMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/site-content")
@Transactional
public class AdminStorefrontContentController {
    private static final long MAX_LOGO_BYTES = 2L * 1024 * 1024;
    private static final long MAX_HOME_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final String ASSET_PATH = "/api/content/site/assets/";

    private final StorefrontSettingsRepository settings;
    private final StorefrontContentAssetRepository assets;
    private final StorefrontContentMapper mapper;

    private final PageLayouts layouts;

    public AdminStorefrontContentController(StorefrontSettingsRepository settings, StorefrontContentAssetRepository assets,
                                            StorefrontContentMapper mapper, PageLayouts layouts) {
        this.layouts = layouts;
        this.settings = settings;
        this.assets = assets;
        this.mapper = mapper;
    }

    @GetMapping
    public StorefrontContentResponse content() {
        return mapper.toResponse(settings.findById(1L).orElseThrow());
    }

    @PutMapping(consumes = "multipart/form-data")
    public StorefrontContentResponse update(
        @Valid @RequestPart("content") StorefrontContentRequest request,
        @RequestPart(value = "logoImage", required = false) MultipartFile logoImage,
        @RequestParam MultiValueMap<String, MultipartFile> homeImages
    ) {
        StorefrontSettings store = forEditing();
        List<StorefrontContentResponse.HomeSection> sections = saveHomeImages(request.homeSections(), homeImages);
        mapper.apply(store, request, sections);
        layouts.updateLegacyHome(sections);
        updateLogo(store, logoImage, request.clearLogo());
        return mapper.toResponse(settings.save(store));
    }

    @PutMapping(value = "/brand", consumes = "multipart/form-data")
    public StorefrontContentResponse updateBrand(
        @Valid @RequestPart("content") StorefrontContentRequest.Brand brand,
        @RequestPart(value = "logoImage", required = false) MultipartFile logoImage
    ) {
        StorefrontSettings store = forEditing();
        mapper.applyBrand(store, brand);
        updateLogo(store, logoImage, brand.clearLogo());
        return mapper.toResponse(settings.save(store));
    }

    @PutMapping(value = "/header", consumes = "multipart/form-data")
    public StorefrontContentResponse updateHeader(@Valid @RequestPart("content") StorefrontContentRequest.Header header) {
        StorefrontSettings store = forEditing();
        mapper.applyHeader(store, header);
        return mapper.toResponse(settings.save(store));
    }

    @PutMapping(value = "/navigation", consumes = "multipart/form-data")
    public StorefrontContentResponse updateNavigation(@Valid @RequestPart("content") StorefrontContentRequest.Navigation navigation) {
        StorefrontSettings store = forEditing();
        mapper.applyNavigation(store, navigation.navigation());
        return mapper.toResponse(settings.save(store));
    }

    @PutMapping(value = "/footer", consumes = "multipart/form-data")
    public StorefrontContentResponse updateFooter(@Valid @RequestPart("content") StorefrontContentRequest.Footer footer) {
        StorefrontSettings store = forEditing();
        mapper.applyFooter(store, footer);
        return mapper.toResponse(settings.save(store));
    }

    @PutMapping(value = "/home", consumes = "multipart/form-data")
    public StorefrontContentResponse updateHome(
        @Valid @RequestPart("content") StorefrontContentRequest.Home home,
        @RequestParam MultiValueMap<String, MultipartFile> uploads
    ) {
        StorefrontSettings store = forEditing();
        var sections = saveHomeImages(home.homeSections(), uploads);
        mapper.applyHome(store, sections);
        layouts.updateLegacyHome(sections);
        return mapper.toResponse(settings.save(store));
    }

    private StorefrontSettings forEditing() {
        return settings.findForEditing().orElseThrow();
    }

    private void updateLogo(StorefrontSettings store, MultipartFile logoImage, boolean clearLogo) {
        UploadedLogo logo = prepareLogo(logoImage);
        if (logo != null) {
            store.setLogoContentType(logo.contentType());
            store.setLogoData(logo.data());
        } else if (clearLogo) {
            store.setLogoContentType(null);
            store.setLogoData(null);
        }
    }

    private List<StorefrontContentResponse.HomeSection> saveHomeImages(
        List<StorefrontContentResponse.HomeSection> sections, MultiValueMap<String, MultipartFile> uploads
    ) {
        List<StorefrontContentResponse.HomeSection> saved = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (StorefrontContentResponse.HomeSection section : sections) {
            String key = section.imageKey() == null ? "" : section.imageKey().trim();
            String imageUrl = section.imageUrl();
            if (!key.isEmpty()) {
                if (!key.matches("[a-z0-9-]{1,80}") || !keys.add(key)) {
                    throw new StoreProblem(HttpStatus.BAD_REQUEST, "homeImageKeysMustBeUniqueLowercaseLetters");
                }
                MultipartFile file = uploads.getFirst("homeImage_" + key);
                if (file != null && !file.isEmpty()) {
                    UploadedLogo upload = prepareImage(file);
                    assets.save(new StorefrontContentAsset(key, upload.contentType(), upload.data()));
                    imageUrl = ASSET_PATH + key;
                } else if (section.clearImage()) {
                    assets.deleteById(key);
                    imageUrl = null;
                } else if (assets.existsById(key)) {
                    imageUrl = ASSET_PATH + key;
                } else if (imageUrl != null && imageUrl.startsWith(ASSET_PATH)) {
                    imageUrl = null;
                }
            } else if (section.clearImage()) {
                imageUrl = null;
            }
            saved.add(new StorefrontContentResponse.HomeSection(section.type(), section.visible(), key, false,
                section.eyebrow(), section.heading(), section.body(), imageUrl, section.imageAlt(), section.imageCaption(),
                section.buttonLabel(), section.buttonPath(), section.buttonCategory(), section.linkLabel(), section.linkPath(),
                section.cardEyebrow(), section.cardLinkLabel(), section.emptyMessage(), section.emptyLinkLabel(), section.items()));
        }
        return saved;
    }

    private UploadedLogo prepareImage(MultipartFile file) {
        if (file.getSize() > MAX_HOME_IMAGE_BYTES) throw new StoreProblem(HttpStatus.PAYLOAD_TOO_LARGE, "eachHomePageImageMustBe8Mb");
        try {
            byte[] data = file.getBytes();
            String contentType = ImageContentType.detect(data);
            if (contentType == null) throw new StoreProblem(HttpStatus.BAD_REQUEST, "uploadAValidJpegPngOrWebpImage");
            return new UploadedLogo(contentType, data);
        } catch (IOException exception) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "aHomePageImageCouldNotBeRead");
        }
    }

    private UploadedLogo prepareLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getSize() > MAX_LOGO_BYTES) throw new StoreProblem(HttpStatus.PAYLOAD_TOO_LARGE, "theLogoMustBe2MbOrSmaller");
        try {
            byte[] data = file.getBytes();
            String contentType = ImageContentType.detect(data);
            if (contentType == null) throw new StoreProblem(HttpStatus.BAD_REQUEST, "uploadAValidPngJpegOrWebpLogo");
            return new UploadedLogo(contentType, data);
        } catch (IOException exception) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "theLogoImageCouldNotBeRead");
        }
    }

    private record UploadedLogo(String contentType, byte[] data) {}
}

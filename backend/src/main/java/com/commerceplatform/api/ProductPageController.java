package com.commerceplatform.api;

import com.commerceplatform.domain.InterfaceText;
import com.commerceplatform.domain.StorefrontContentAsset;
import com.commerceplatform.repository.InterfaceTextRepository;
import com.commerceplatform.repository.StorefrontContentAssetRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class ProductPageController {
    private static final String PREFIX = "storefront.";
    private static final String COPY = "product-detail.";
    private static final String ICONS = "product-icons.";
    private final InterfaceTextRepository texts;
    private final StorefrontContentAssetRepository assets;

    public ProductPageController(InterfaceTextRepository texts, StorefrontContentAssetRepository assets) {
        this.texts = texts;
        this.assets = assets;
    }

    public record Template(Map<String, String> texts, Map<String, String> symbols, Map<String, String> images) {}
    public record Edit(@NotNull @Size(max = 180) Map<String, @NotNull @Size(max = 2000) String> texts,
                       @NotNull @Size(max = 32) Map<String, @NotNull @Size(max = 32) String> symbols,
                       @NotNull @Size(max = 32) Set<String> removeImages) {}

    @GetMapping("/content/product-page")
    public Template template() {
        Map<String, String> images = new LinkedHashMap<>();
        for (String key : assets.productIconKeys()) images.put(key.substring(4), "/api/content/site/assets/" + key);
        return new Template(values(COPY), values(ICONS), images);
    }

    @Transactional
    @PutMapping(value = "/admin/product-page", consumes = "multipart/form-data")
    public Template publish(@Valid @RequestPart("changes") Edit edit, MultipartHttpServletRequest request) {
        update(COPY, edit.texts());
        update(ICONS, edit.symbols());
        var slots = values(ICONS).keySet();
        for (String slot : edit.removeImages()) {
            requireSlot(slots, slot);
            if (request.getFile("icon_" + slot) != null) throw new StoreProblem(HttpStatus.BAD_REQUEST, "invalidInput");
            assets.deleteById("pdp." + slot);
        }
        for (var part : request.getMultiFileMap().entrySet()) {
            if (part.getKey().equals("changes")) continue;
            if (!part.getKey().startsWith("icon_") || part.getValue().size() != 1) throw new StoreProblem(HttpStatus.BAD_REQUEST, "invalidInput");
            String slot = part.getKey().substring("icon_".length());
            requireSlot(slots, slot);
            storeImage("pdp." + slot, part.getValue().get(0));
        }
        return template();
    }

    private Map<String, String> values(String group) {
        String prefix = PREFIX + group;
        Map<String, String> result = new LinkedHashMap<>();
        texts.findByIdStartingWithOrderById(prefix).forEach(row -> result.put(row.getId().substring(prefix.length()), row.getWording()));
        return result;
    }

    private void update(String group, Map<String, String> changes) {
        if (changes.isEmpty()) return;
        String prefix = PREFIX + group;
        List<InterfaceText> rows = texts.findAllById(changes.keySet().stream().map(key -> prefix + key).toList());
        if (rows.size() != changes.size()) throw new StoreProblem(HttpStatus.BAD_REQUEST, "unknownInterfaceText");
        rows.forEach(row -> row.setWording(changes.get(row.getId().substring(prefix.length()))));
    }

    private void requireSlot(Set<String> slots, String slot) {
        if (!slots.contains(slot)) throw new StoreProblem(HttpStatus.BAD_REQUEST, "unknownInterfaceText");
    }

    private void storeImage(String key, MultipartFile file) {
        if (file.getSize() > 2L * 1024 * 1024) throw new StoreProblem(HttpStatus.PAYLOAD_TOO_LARGE, "iconTooLarge");
        try {
            byte[] data = file.getBytes();
            String type = ImageContentType.detect(data);
            if (type == null) throw new StoreProblem(HttpStatus.BAD_REQUEST, "uploadAValidJpegPngOrWebpImage");
            assets.save(new StorefrontContentAsset(key, type, data));
        } catch (IOException failure) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "theImageCouldNotBeRead");
        }
    }
}

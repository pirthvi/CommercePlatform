package com.commerceplatform.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.commerceplatform.domain.CatalogCategory;
import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.ProductImage;
import com.commerceplatform.domain.StorefrontContentAsset;
import com.commerceplatform.repository.CategoryRepository;
import com.commerceplatform.repository.ProductImageRepository;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.StorefrontContentAssetRepository;
import com.commerceplatform.service.CategoryService;
import com.commerceplatform.service.InventoryService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
class ApparelCatalog {
    record Selection(Map<String, String> categorySlugs, List<Long> productIds) {}
    private record Group(String name, String description, List<Style> products) {}
    private record Style(String name, String slug, String photo, String look, BigDecimal price, int stock,
                         String size, String description, String materials, String care, boolean featured) {}

    private final ObjectMapper json;
    private final CategoryRepository categories;
    private final CategoryService categoryService;
    private final ProductRepository products;
    private final ProductImageRepository photos;
    private final StorefrontContentAssetRepository assets;
    private final InventoryService inventory;

    ApparelCatalog(ObjectMapper json, CategoryRepository categories, CategoryService categoryService,
                   ProductRepository products, ProductImageRepository photos, StorefrontContentAssetRepository assets,
                   InventoryService inventory) {
        this.json = json;
        this.categories = categories;
        this.categoryService = categoryService;
        this.products = products;
        this.photos = photos;
        this.assets = assets;
        this.inventory = inventory;
    }

    Selection install() throws IOException {
        List<Group> groups;
        try (var stream = new ClassPathResource("commerce/starter-catalog.json").getInputStream()) {
            groups = json.readValue(stream, new TypeReference<>() {});
        }
        var slugs = new LinkedHashMap<String, String>();
        var ids = new ArrayList<Long>();
        for (var group : groups) {
            var category = categories.findByNormalizedName(group.name().toLowerCase(Locale.ROOT))
                .orElseGet(() -> categoryService.create(group.name(), group.description()));
            slugs.put(group.name(), category.getSlug());
            for (var style : group.products()) {
                var product = products.findBySlug(style.slug()).orElse(null);
                if (product == null) product = create(style, category);
                ids.add(product.getId());
            }
        }
        return new Selection(slugs, ids);
    }

    private Product create(Style style, CatalogCategory category) throws IOException {
        var product = new Product(style.name(), style.slug(), category.getName(), style.description(),
            style.price(), 0, "", null, style.featured(), true);
        product.assignCategory(category);
        product.setWeight(style.size());
        product.setIngredients(style.materials());
        product.setPairings(style.care());
        products.save(product);
        inventory.change(product, style.stock(), "opening");
        product.setImageUrl(storeProductPhoto(product, "MAIN", style.photo()));
        product.setGalleryImages(storeProductPhoto(product, "GALLERY", style.look()));
        return product;
    }

    private String storeProductPhoto(Product product, String variant, String name) throws IOException {
        return photos.save(new ProductImage(product.getId(), variant, 0, "image/jpeg", bytes(name))).resourcePath();
    }

    String pageImage(String name) throws IOException {
        String key = "apparel.v1." + name;
        if (!assets.existsById(key)) assets.save(new StorefrontContentAsset(key, "image/jpeg", bytes(name)));
        return "/api/content/site/assets/" + key;
    }

    private byte[] bytes(String name) throws IOException {
        return new ClassPathResource("commerce/apparel/" + name + ".jpg").getContentAsByteArray();
    }
}

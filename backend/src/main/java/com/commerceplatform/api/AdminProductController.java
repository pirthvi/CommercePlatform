package com.commerceplatform.api;

import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.ProductImage;
import com.commerceplatform.repository.ProductImageRepository;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.service.CategoryService;
import com.commerceplatform.service.InventoryService;
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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@Transactional
public class AdminProductController {
    private static final long MAX_IMAGE_BYTES = 8L * 1024 * 1024;
    private static final int MAX_GALLERY_IMAGES = 8;

    private final ProductRepository products;
    private final ProductImageRepository images;

    private final CategoryService categories;
    private final InventoryService inventory;

    public AdminProductController(ProductRepository products, ProductImageRepository images, CategoryService categories, InventoryService inventory) {
        this.products = products;
        this.images = images;
        this.categories = categories;
        this.inventory = inventory;
    }

    @GetMapping
    public List<Product> list() { return products.findAll(); }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(
        @Valid @RequestPart("product") ProductRequest body,
        @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
        @RequestPart(value = "galleryImages", required = false) List<MultipartFile> galleryImages
    ) {
        UploadedImage main = prepare(mainImage, true);
        List<UploadedImage> gallery = prepareGallery(galleryImages);
        var category = categories.resolve(body.categoryId(), body.category());
        Product product = new Product(body.name(), body.slug(), category.getName(), body.description(), body.price(), 0, "", body.badge(), body.featured(), body.active());
        product.assignCategory(category);
        applyDetails(product, body);
        products.save(product);
        inventory.change(product, body.stock(), "opening");
        saveMainImage(product, main);
        saveGalleryImages(product, gallery, true);
        return products.save(product);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public Product update(
        @PathVariable Long id,
        @Valid @RequestPart("product") ProductRequest body,
        @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
        @RequestPart(value = "galleryImages", required = false) List<MultipartFile> galleryImages
    ) {
        Product product = inventory.lock(id);
        UploadedImage preparedMain = prepare(mainImage, false);
        List<UploadedImage> preparedGallery = prepareGallery(galleryImages);
        product.setName(body.name());
        product.setSlug(body.slug());
        product.assignCategory(categories.resolve(body.categoryId(), body.category()));
        product.setDescription(body.description());
        product.setPrice(body.price());
        if (!body.stock().equals(body.previousStock())) {
            if (body.previousStock() != null && !body.previousStock().equals(product.getStock())) {
                throw new StoreProblem(HttpStatus.CONFLICT, "stockChangedWhileThisProductWasOpenReopen");
            }
            inventory.change(product, body.stock() - product.getStock(), "product_edit");
        }
        product.setBadge(body.badge());
        applyDetails(product, body);
        product.setFeatured(body.featured());
        product.setActive(body.active());

        if (preparedMain != null) saveMainImage(product, preparedMain);
        if (body.replaceGallery() || !preparedGallery.isEmpty()) {
            saveGalleryImages(product, preparedGallery, true);
        }
        return products.save(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!products.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        images.deleteByProductId(id);
        products.deleteById(id);
    }

    private void applyDetails(Product product, ProductRequest body) {
        product.setWeight(body.weight());
        product.setOrigin(body.origin());
        product.setTastingNotes(body.tastingNotes());
        product.setPairings(body.pairings());
        product.setIngredients(body.ingredients());
        product.setHighlights(body.highlights());
    }

    private void saveMainImage(Product product, UploadedImage image) {
        images.deleteByProductIdAndVariant(product.getId(), "MAIN");
        ProductImage stored = images.save(new ProductImage(product.getId(), "MAIN", 0, image.contentType(), image.data()));
        product.setImageUrl(stored.resourcePath());
    }

    private void saveGalleryImages(Product product, List<UploadedImage> uploads, boolean replaceExisting) {
        if (replaceExisting) images.deleteByProductIdAndVariant(product.getId(), "GALLERY");
        List<String> urls = new ArrayList<>();
        int position = 0;
        for (UploadedImage upload : uploads) {
            ProductImage stored = images.save(new ProductImage(product.getId(), "GALLERY", position++, upload.contentType(), upload.data()));
            urls.add(stored.resourcePath());
        }
        product.setGalleryImages(urls.isEmpty() ? null : String.join("\n", urls));
    }

    private List<UploadedImage> prepareGallery(List<MultipartFile> files) {
        if (files == null) return List.of();
        List<UploadedImage> prepared = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            prepared.add(prepare(file, true));
        }
        if (prepared.size() > MAX_GALLERY_IMAGES) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "chooseUpTo8GalleryImagesAtA");
        }
        return prepared;
    }

    private UploadedImage prepare(MultipartFile file, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required) throw new StoreProblem(HttpStatus.BAD_REQUEST, "chooseAMainProductImage");
            return null;
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new StoreProblem(HttpStatus.PAYLOAD_TOO_LARGE, "eachImageMustBe8MbOrSmaller");
        }
        try {
            byte[] data = file.getBytes();
            String contentType = detectImageType(data);
            if (contentType == null) {
                throw new StoreProblem(HttpStatus.BAD_REQUEST, "uploadAValidJpegPngOrWebpImage");
            }
            return new UploadedImage(contentType, data);
        } catch (IOException exception) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "theImageCouldNotBeRead");
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

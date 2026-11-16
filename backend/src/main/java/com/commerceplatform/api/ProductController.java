package com.commerceplatform.api;

import com.commerceplatform.domain.Product;
import com.commerceplatform.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository products;
    public ProductController(ProductRepository products) { this.products = products; }
    @GetMapping public List<Product> list(@RequestParam(required = false) Long categoryId) {
        return categoryId == null ? products.findByActiveTrueOrderByFeaturedDescNameAsc()
            : products.findByActiveTrueAndCatalogCategory_IdOrderByFeaturedDescNameAsc(categoryId);
    }
    @GetMapping("/slug/{slug}") public Product getBySlug(@PathVariable String slug) {
        Product product = products.findBySlug(slug).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!product.isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return product;
    }
    @GetMapping("/{id}") public Product get(@PathVariable Long id) {
        Product product = products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!product.isActive()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return product;
    }
}

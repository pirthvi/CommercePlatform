package com.commerceplatform.service;

import com.commerceplatform.api.StoreProblem;

import com.commerceplatform.domain.CatalogCategory;
import com.commerceplatform.repository.CategoryRepository;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.StorefrontSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.Locale;

@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categories;
    private final ProductRepository products;

    private final StorefrontSettingsRepository sites;
    private final StorefrontContentMapper content;
    private final PageLayouts layouts;

    public CategoryService(CategoryRepository categories, ProductRepository products, StorefrontSettingsRepository sites, StorefrontContentMapper content, PageLayouts layouts) {
        this.layouts = layouts;
        this.sites = sites;
        this.content = content;
        this.categories = categories;
        this.products = products;
    }
    public CatalogCategory require(long id) {
        return categories.findById(id).orElseThrow(() -> new StoreProblem(HttpStatus.NOT_FOUND, "categoryNotFound"));
    }
    public CatalogCategory create(String name, String description) {
        if (categories.findByNormalizedName(name.strip().toLowerCase(Locale.ROOT)).isPresent()) {
            throw new StoreProblem(HttpStatus.CONFLICT, "aCategoryWithThisNameAlreadyExists");
        }
        String base = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isEmpty()) base = "category";
        String slug = base;
        for (int suffix = 2; categories.existsBySlug(slug); suffix++) slug = base + "-" + suffix;
        return categories.save(new CatalogCategory(name, slug, description));
    }
    public CatalogCategory resolve(Long id, String legacyName) {
        if (id != null) return require(id);
        if (legacyName == null || legacyName.isBlank()) {
            throw new StoreProblem(HttpStatus.BAD_REQUEST, "chooseACategory");
        }
        return categories.findByNormalizedName(legacyName.strip().toLowerCase(Locale.ROOT)).orElseGet(() -> create(legacyName, ""));
    }
    public CatalogCategory edit(long id, String name, String description) {
        CatalogCategory category = require(id);
        if (categories.findByNormalizedName(name.strip().toLowerCase(Locale.ROOT)).filter(other -> !other.getId().equals(id)).isPresent()) {
            throw new StoreProblem(HttpStatus.CONFLICT, "aCategoryWithThisNameAlreadyExists");
        }
        category.edit(name, description);
        return category;
    }
    public void delete(long id) {
        CatalogCategory category = require(id);
        if (products.existsByCatalogCategory_Id(id)) throw new StoreProblem(HttpStatus.CONFLICT, "reassignThisCategorySProductsBeforeDeletingIt");
        sites.findForEditing().ifPresent(site -> {
            var layout = content.toResponse(site);
            String slug = category.getSlug();
            boolean inMenu = layout.navigation().stream().anyMatch(link -> slug.equals(link.category()));
            boolean inFooter = layout.footerColumns().stream().flatMap(column -> column.links().stream()).anyMatch(link -> slug.equals(link.category()));
            boolean onHome = layout.homeSections().stream().anyMatch(section -> slug.equals(section.buttonCategory())
                || section.items().stream().anyMatch(item -> slug.equals(item.category())));
            if (inMenu || inFooter || onHome || layouts.referencesCategory(id, slug)) throw new StoreProblem(HttpStatus.CONFLICT, "categoryInContent");
        });
        categories.delete(category);
    }
}

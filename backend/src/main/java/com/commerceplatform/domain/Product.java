package com.commerceplatform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 140)
    private String slug;
    @Column(nullable = false, length = 80)
    private String category;
    @ManyToOne
    @JoinColumn(name = "category_id")
    private CatalogCategory catalogCategory;
    @Column(nullable = false, length = 2000)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private Integer stock;
    @Column(nullable = false, length = 700)
    private String imageUrl;
    @Column(length = 3000)
    private String galleryImages;
    @Column(length = 120)
    private String weight;
    @Column(length = 500)
    private String origin;
    @Column(length = 500)
    private String tastingNotes;
    @Column(length = 800)
    private String pairings;
    @Column(length = 500)
    private String ingredients;
    @Column(length = 1200)
    private String highlights;
    @Column(length = 40)
    private String badge;
    @Column(nullable = false)
    private boolean featured;
    @Column(nullable = false)
    private boolean active = true;

    protected Product() {}

    public Product(String name, String slug, String category, String description, BigDecimal price, Integer stock, String imageUrl, String badge, boolean featured, boolean active) {
        this.name = name; this.slug = slug; this.category = category; this.description = description;
        this.price = price; this.stock = stock; this.imageUrl = imageUrl; this.badge = badge;
        this.featured = featured; this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getCategory() { return catalogCategory == null ? category : catalogCategory.getName(); }
    public Long getCategoryId() { return catalogCategory == null ? null : catalogCategory.getId(); }
    public String getCategorySlug() { return catalogCategory == null ? null : catalogCategory.getSlug(); }
    public void assignCategory(CatalogCategory value) {
        catalogCategory = value;
        category = value.getName();
    }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getGalleryImages() { return galleryImages; }
    public void setGalleryImages(String galleryImages) { this.galleryImages = galleryImages; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getTastingNotes() { return tastingNotes; }
    public void setTastingNotes(String tastingNotes) { this.tastingNotes = tastingNotes; }
    public String getPairings() { return pairings; }
    public void setPairings(String pairings) { this.pairings = pairings; }
    public String getIngredients() { return ingredients; }
    public void setIngredients(String ingredients) { this.ingredients = ingredients; }
    public String getHighlights() { return highlights; }
    public void setHighlights(String highlights) { this.highlights = highlights; }
    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

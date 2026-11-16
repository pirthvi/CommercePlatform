package com.commerceplatform.domain;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Locale;

@Entity
@Table(name = "catalog_categories")
public class CatalogCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String name;
    @Column(nullable = false, unique = true, length = 100) private String normalizedName;
    @Column(nullable = false, unique = true, length = 120) private String slug;
    @Column(nullable = false, length = 500) private String description;

    protected CatalogCategory() {}
    public CatalogCategory(String name, String slug, String description) {
        this.slug = slug;
        edit(name, description);
    }
    public void edit(String name, String description) {
        this.name = name.strip();
        this.normalizedName = this.name.toLowerCase(Locale.ROOT);
        this.description = description == null ? "" : description.strip();
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    @JsonIgnore public String getNormalizedName() { return normalizedName; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
}

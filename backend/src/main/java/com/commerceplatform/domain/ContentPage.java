package com.commerceplatform.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "content_pages")
public class ContentPage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 140)
    private String title;
    @Column(nullable = false, unique = true, length = 120)
    private String slug;
    @Column(length = 140)
    private String eyebrow;
    @Column(nullable = false, length = 12000)
    private String body;
    @Column(length = 240)
    private String metaDescription;
    @Column(length = 700)
    private String heroImageUrl;
    @Column(length = 100)
    private String buttonLabel;
    @Column(length = 240)
    private String buttonLink;
    @Column(nullable = false)
    private boolean published = true;
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(unique = true, length = 32)
    private String templateKey;
    @Lob
    private String layoutJson;
    private Integer layoutRevision;

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String value) { templateKey = value; }
    public int getLayoutRevision() { return layoutRevision == null ? 0 : layoutRevision; }
    @JsonIgnore
    public String getLayoutJson() { return layoutJson; }
    public void replaceLayout(String value) {
        layoutJson = value;
        layoutRevision = getLayoutRevision() + 1;
    }

    protected ContentPage() {}
    public ContentPage(String title, String slug, String eyebrow, String body, String metaDescription, boolean published) {
        this(title, slug, eyebrow, body, metaDescription, published, null, null, null);
    }
    public ContentPage(String title, String slug, String eyebrow, String body, String metaDescription, boolean published,
                       String heroImageUrl, String buttonLabel, String buttonLink) {
        this.title = title; this.slug = slug; this.eyebrow = eyebrow; this.body = body;
        this.metaDescription = metaDescription; this.published = published; this.updatedAt = Instant.now();
        this.heroImageUrl = heroImageUrl; this.buttonLabel = buttonLabel; this.buttonLink = buttonLink;
    }
    @PreUpdate public void touch() { this.updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getEyebrow() { return eyebrow; }
    public void setEyebrow(String eyebrow) { this.eyebrow = eyebrow; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getMetaDescription() { return metaDescription; }
    public void setMetaDescription(String metaDescription) { this.metaDescription = metaDescription; }
    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }
    public String getButtonLabel() { return buttonLabel; }
    public void setButtonLabel(String buttonLabel) { this.buttonLabel = buttonLabel; }
    public String getButtonLink() { return buttonLink; }
    public void setButtonLink(String buttonLink) { this.buttonLink = buttonLink; }
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    public Instant getUpdatedAt() { return updatedAt; }
}

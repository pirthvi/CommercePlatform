package com.commerceplatform.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_product_images_product_variant", columnList = "product_id, variant")
})
public class ProductImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 16)
    private String variant;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    protected ProductImage() {}

    public ProductImage(Long productId, String variant, Integer position, String contentType, byte[] data) {
        this.productId = productId;
        this.variant = variant;
        this.position = position;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getId() { return id; }
    public String resourcePath() { return "/api/products/" + productId + "/images/" + id; }
    public Long getProductId() { return productId; }
    public String getVariant() { return variant; }
    public Integer getPosition() { return position; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}

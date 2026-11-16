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
@Table(name = "content_page_images", indexes = {
    @Index(name = "idx_content_page_images_page", columnList = "page_id")
})
public class ContentPageImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_id", nullable = false)
    private Long pageId;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    protected ContentPageImage() {}

    public ContentPageImage(Long pageId, String contentType, byte[] data) {
        this.pageId = pageId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getId() { return id; }
    public Long getPageId() { return pageId; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}

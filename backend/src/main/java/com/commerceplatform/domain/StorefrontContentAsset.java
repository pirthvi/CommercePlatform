package com.commerceplatform.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "storefront_content_assets")
public class StorefrontContentAsset {
    @Id
    @Column(name = "asset_key", length = 80)
    private String assetKey;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false)
    private byte[] data;

    protected StorefrontContentAsset() {}

    public StorefrontContentAsset(String assetKey, String contentType, byte[] data) {
        this.assetKey = assetKey;
        this.contentType = contentType;
        this.data = data;
    }

    public String getAssetKey() { return assetKey; }
    public String getContentType() { return contentType; }
    public byte[] getData() { return data; }
}

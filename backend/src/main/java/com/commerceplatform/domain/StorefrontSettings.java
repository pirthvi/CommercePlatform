package com.commerceplatform.domain;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "storefront_settings")
public class StorefrontSettings {
    @Id
    private Long id = 1L;
    private Integer apparelStarterVersion;

    public boolean hasApparelStarter(int version) {
        return apparelStarterVersion != null && apparelStarterVersion >= version;
    }

    public void recordApparelStarter(int version) {
        apparelStarterVersion = version;
    }

    @Column(name = "announcement_text", nullable = false, length = 240)
    private String announcementText;
    @Column(name = "announcement_secondary", nullable = false, length = 240)
    private String announcementSecondary;
    @Column(name = "brand_mark", nullable = false, length = 32)
    private String brandMark;
    @Column(name = "brand_name", nullable = false, length = 120)
    private String brandName;
    @Column(name = "brand_tagline", nullable = false, length = 160)
    private String brandTagline;
    @Column(name = "search_label", nullable = false, length = 40)
    private String searchLabel;
    @Column(name = "sign_in_label", nullable = false, length = 40)
    private String signInLabel;
    @Column(name = "account_label", nullable = false, length = 40)
    private String accountLabel;
    @Column(name = "bag_label", nullable = false, length = 40)
    private String bagLabel;
    @Column(name = "footer_story", nullable = false, length = 1200)
    private String footerStory;

    @Lob @Column(name = "navigation_json", nullable = false)
    private String navigationJson;
    @Lob @Column(name = "footer_columns_json", nullable = false)
    private String footerColumnsJson;
    @Lob @Column(name = "footer_bottom_json", nullable = false)
    private String footerBottomJson;
    @Lob @Column(name = "home_sections_json", nullable = false)
    private String homeSectionsJson;

    @Column(name = "logo_content_type", length = 40)
    private String logoContentType;
    @Lob @Basic(fetch = FetchType.LAZY) @Column(name = "logo_data")
    private byte[] logoData;

    protected StorefrontSettings() {}

    public static StorefrontSettings create() { return new StorefrontSettings(); }

    public Long getId() { return id; }
    public String getAnnouncementText() { return announcementText; }
    public void setAnnouncementText(String value) { this.announcementText = value; }
    public String getAnnouncementSecondary() { return announcementSecondary; }
    public void setAnnouncementSecondary(String value) { this.announcementSecondary = value; }
    public String getBrandMark() { return brandMark; }
    public void setBrandMark(String value) { this.brandMark = value; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String value) { this.brandName = value; }
    public String getBrandTagline() { return brandTagline; }
    public void setBrandTagline(String value) { this.brandTagline = value; }
    public String getSearchLabel() { return searchLabel; }
    public void setSearchLabel(String value) { this.searchLabel = value; }
    public String getSignInLabel() { return signInLabel; }
    public void setSignInLabel(String value) { this.signInLabel = value; }
    public String getAccountLabel() { return accountLabel; }
    public void setAccountLabel(String value) { this.accountLabel = value; }
    public String getBagLabel() { return bagLabel; }
    public void setBagLabel(String value) { this.bagLabel = value; }
    public String getFooterStory() { return footerStory; }
    public void setFooterStory(String value) { this.footerStory = value; }
    public String getNavigationJson() { return navigationJson; }
    public void setNavigationJson(String value) { this.navigationJson = value; }
    public String getFooterColumnsJson() { return footerColumnsJson; }
    public void setFooterColumnsJson(String value) { this.footerColumnsJson = value; }
    public String getFooterBottomJson() { return footerBottomJson; }
    public void setFooterBottomJson(String value) { this.footerBottomJson = value; }
    public String getHomeSectionsJson() { return homeSectionsJson; }
    public void setHomeSectionsJson(String value) { this.homeSectionsJson = value; }
    public String getLogoContentType() { return logoContentType; }
    public void setLogoContentType(String value) { this.logoContentType = value; }
    public byte[] getLogoData() { return logoData; }
    public void setLogoData(byte[] value) { this.logoData = value; }
}

package com.commerceplatform.api;

import java.util.List;

public record StorefrontContentResponse(
    String announcementText,
    String announcementSecondary,
    String brandMark,
    String brandName,
    String brandTagline,
    String searchLabel,
    String signInLabel,
    String accountLabel,
    String bagLabel,
    String logoUrl,
    String footerStory,
    List<NavigationItem> navigation,
    List<FooterColumn> footerColumns,
    List<String> footerBottom,
    List<HomeSection> homeSections
) {
    public record NavigationItem(String label, String path, String category) {}
    public record FooterLink(String label, String path, String category) {}
    public record FooterColumn(String heading, List<FooterLink> links) {
        public FooterColumn { if (links == null) links = List.of(); }
    }
    public record HomeItem(String icon, String title, String body, String category, String path) {}
    public record HomeSection(
        String type,
        boolean visible,
        String color,
        String imageKey,
        boolean clearImage,
        String eyebrow,
        String heading,
        String body,
        String imageUrl,
        String imageAlt,
        String imageCaption,
        String buttonLabel,
        String buttonPath,
        String buttonCategory,
        String linkLabel,
        String linkPath,
        String cardEyebrow,
        String cardLinkLabel,
        String emptyMessage,
        String emptyLinkLabel,
        List<HomeItem> items
    ) {
        public HomeSection { if (items == null) items = List.of(); }
    }
}

package com.commerceplatform.api;

import com.commerceplatform.api.StorefrontContentResponse.FooterColumn;
import com.commerceplatform.api.StorefrontContentResponse.HomeSection;
import com.commerceplatform.api.StorefrontContentResponse.NavigationItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StorefrontContentRequest(
    @NotNull @Size(max = 240) String announcementText,
    @Size(max = 240) String announcementSecondary,
    @NotBlank @Size(max = 32) String brandMark,
    @NotBlank @Size(max = 120) String brandName,
    @Size(max = 160) String brandTagline,
    @Size(max = 40) String searchLabel,
    @Size(max = 40) String signInLabel,
    @Size(max = 40) String accountLabel,
    @Size(max = 40) String bagLabel,
    @NotNull @Size(max = 1200) String footerStory,
    @NotNull List<NavigationItem> navigation,
    @NotNull List<FooterColumn> footerColumns,
    @NotNull List<String> footerBottom,
    @NotNull List<HomeSection> homeSections,
    boolean clearLogo
) {
    public record Brand(
        @NotBlank @Size(max = 32) String brandMark,
        @NotBlank @Size(max = 120) String brandName,
        @NotNull @Size(max = 160) String brandTagline,
        boolean clearLogo
    ) {}

    public record Header(
        @NotNull @Size(max = 240) String announcementText,
        @NotNull @Size(max = 240) String announcementSecondary,
        @NotNull @Size(max = 40) String searchLabel,
        @NotNull @Size(max = 40) String signInLabel,
        @NotNull @Size(max = 40) String accountLabel,
        @NotNull @Size(max = 40) String bagLabel
    ) {}

    public record Navigation(@NotNull List<@NotNull NavigationItem> navigation) {}

    public record Footer(
        @NotNull @Size(max = 1200) String footerStory,
        @NotNull List<@NotNull FooterColumn> footerColumns,
        @NotNull List<@NotNull String> footerBottom
    ) {}

    public record Home(@NotNull List<@NotNull HomeSection> homeSections) {}
}

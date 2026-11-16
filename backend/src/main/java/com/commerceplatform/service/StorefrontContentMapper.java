package com.commerceplatform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.commerceplatform.api.StorefrontContentRequest;
import com.commerceplatform.api.StorefrontContentResponse;
import com.commerceplatform.domain.StorefrontSettings;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StorefrontContentMapper {
    private static final TypeReference<List<StorefrontContentResponse.NavigationItem>> NAVIGATION = new TypeReference<>() {};
    private static final TypeReference<List<StorefrontContentResponse.FooterColumn>> FOOTER_COLUMNS = new TypeReference<>() {};
    private static final TypeReference<List<String>> FOOTER_BOTTOM = new TypeReference<>() {};
    private static final TypeReference<List<StorefrontContentResponse.HomeSection>> HOME_SECTIONS = new TypeReference<>() {};

    private final ObjectMapper mapper;

    public StorefrontContentMapper(ObjectMapper mapper) { this.mapper = mapper; }

    public StorefrontContentResponse toResponse(StorefrontSettings settings) {
        return new StorefrontContentResponse(
            settings.getAnnouncementText(), settings.getAnnouncementSecondary(), settings.getBrandMark(),
            settings.getBrandName(), settings.getBrandTagline(), settings.getSearchLabel(), settings.getSignInLabel(),
            settings.getAccountLabel(), settings.getBagLabel(), settings.getLogoData() == null ? null : "/api/content/site/logo",
            settings.getFooterStory(), read(settings.getNavigationJson(), NAVIGATION),
            read(settings.getFooterColumnsJson(), FOOTER_COLUMNS), read(settings.getFooterBottomJson(), FOOTER_BOTTOM),
            read(settings.getHomeSectionsJson(), HOME_SECTIONS)
        );
    }

    public void apply(StorefrontSettings settings, StorefrontContentRequest request,
                      List<StorefrontContentResponse.HomeSection> homeSections) {
        applyBrand(settings, new StorefrontContentRequest.Brand(request.brandMark(), request.brandName(),
            request.brandTagline(), request.clearLogo()));
        applyHeader(settings, new StorefrontContentRequest.Header(request.announcementText(), request.announcementSecondary(),
            request.searchLabel(), request.signInLabel(), request.accountLabel(), request.bagLabel()));
        applyNavigation(settings, request.navigation());
        applyFooter(settings, new StorefrontContentRequest.Footer(request.footerStory(), request.footerColumns(), request.footerBottom()));
        applyHome(settings, homeSections);
    }

    public void applyHeader(StorefrontSettings settings, StorefrontContentRequest.Header request) {
        settings.setAnnouncementText(request.announcementText());
        settings.setAnnouncementSecondary(request.announcementSecondary());
        settings.setSearchLabel(request.searchLabel());
        settings.setSignInLabel(request.signInLabel());
        settings.setAccountLabel(request.accountLabel());
        settings.setBagLabel(request.bagLabel());
    }

    public void applyBrand(StorefrontSettings settings, StorefrontContentRequest.Brand request) {
        settings.setBrandMark(request.brandMark());
        settings.setBrandName(request.brandName());
        settings.setBrandTagline(request.brandTagline());
    }

    public void applyNavigation(StorefrontSettings settings, List<StorefrontContentResponse.NavigationItem> navigation) {
        settings.setNavigationJson(write(navigation));
    }

    public void applyFooter(StorefrontSettings settings, StorefrontContentRequest.Footer request) {
        settings.setFooterStory(request.footerStory());
        settings.setFooterColumnsJson(write(request.footerColumns()));
        settings.setFooterBottomJson(write(request.footerBottom()));
    }

    public void applyHome(StorefrontSettings settings, List<StorefrontContentResponse.HomeSection> homeSections) {
        settings.setHomeSectionsJson(write(homeSections));
    }

    private <T> T read(String value, TypeReference<T> type) {
        try { return mapper.readValue(value, type); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Stored storefront content is not valid JSON", exception); }
    }

    private String write(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Storefront content could not be saved", exception); }
    }
}

package com.commerceplatform.api;

import com.commerceplatform.domain.CommerceSettings;
import com.commerceplatform.domain.StorefrontSettings;
import com.commerceplatform.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Transactional(readOnly = true)
public class StoreConfigurationController {
    private final InterfaceTextRepository texts;
    private final CommerceSettingsRepository commerce;
    private final StorefrontSettingsRepository sites;

    public StoreConfigurationController(InterfaceTextRepository texts, CommerceSettingsRepository commerce, StorefrontSettingsRepository sites) {
        this.texts = texts;
        this.commerce = commerce;
        this.sites = sites;
    }
    public record InterfaceConfig(String brandName, String brandMark, String brandTagline, String logoUrl,
                                  CommerceSettings commerce, Map<String,String> texts, int pageLayoutVersion) {}
    public record TextEdit(@NotEmpty @Size(max = 1500) Map<@NotBlank String, @NotNull @Size(max = 2000) String> values) {}
    public record ShopEdit(@NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
                           @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal deliveryFee,
                           @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal freeDeliveryFrom) {}

    private String prefix(String surface) {
        if (!surface.equals("storefront") && !surface.equals("studio")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return surface + ".";
    }
    @GetMapping("/content/interface/{surface}")
    public InterfaceConfig read(@PathVariable String surface) {
        String prefix = prefix(surface);
        StorefrontSettings site = sites.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        var options = commerce.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
        Map<String,String> values = new LinkedHashMap<>();
        texts.findByIdStartingWithOrderById(prefix).forEach(text -> values.put(text.getId().substring(prefix.length()), text.getWording()));
        return new InterfaceConfig(site.getBrandName(), site.getBrandMark(), site.getBrandTagline(),
            site.getLogoContentType() == null ? null : "/api/content/site/logo", options, values, 1);
    }
    @Transactional @PatchMapping("/admin/interface/{surface}")
    public void updateTexts(@PathVariable String surface, @Valid @RequestBody TextEdit input) {
        String prefix = prefix(surface);
        var rows = texts.findAllById(input.values().keySet().stream().map(key -> prefix + key).toList());
        if (rows.size() != input.values().size()) throw new StoreProblem(HttpStatus.BAD_REQUEST, "unknownInterfaceText");
        rows.forEach(row -> row.setWording(input.values().get(row.getId().substring(prefix.length()))));
    }
    @Transactional @PutMapping("/admin/commerce")
    public CommerceSettings configure(@Valid @RequestBody ShopEdit input) {
        try { Currency.getInstance(input.currency()); }
        catch (IllegalArgumentException invalid) { throw new StoreProblem(HttpStatus.BAD_REQUEST, "chooseAValidIsoCurrencyCode"); }
        var settings = commerce.findById(1L).orElseThrow();
        settings.configure(input.currency(), input.deliveryFee(), input.freeDeliveryFrom());
        return settings;
    }
}

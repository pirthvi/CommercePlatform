package com.commerceplatform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "commerce_settings")
public class CommerceSettings {
    @Id private Long id = 1L;
    @Column(nullable = false, length = 3) private String currency = "PKR";
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal deliveryFee = new BigDecimal("250.00");
    @Column(precision = 12, scale = 2) private BigDecimal freeDeliveryFrom = new BigDecimal("5000.00");
    @Column(length = 7) private String themeColor = "#9A7540";
    @Column(length = 7) private String componentColor = "#536344";
    @Column(length = 24) private String fontFamily = "brand";
    public String getCurrency() { return currency; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getFreeDeliveryFrom() { return freeDeliveryFrom; }
    public String getThemeColor() { return themeColor == null ? "#9A7540" : themeColor; }
    public String getComponentColor() { return componentColor == null ? "#536344" : componentColor; }
    public String getFontFamily() { return fontFamily == null ? "brand" : fontFamily; }
    public void configure(String currency, BigDecimal deliveryFee, BigDecimal freeDeliveryFrom, String themeColor, String componentColor, String fontFamily) {
        this.currency = currency;
        this.deliveryFee = deliveryFee;
        this.freeDeliveryFrom = freeDeliveryFrom;
        this.themeColor = themeColor;
        this.componentColor = componentColor;
        this.fontFamily = fontFamily;
    }
    public BigDecimal shippingFor(BigDecimal subtotal) {
        return freeDeliveryFrom != null && subtotal.compareTo(freeDeliveryFrom) >= 0 ? BigDecimal.ZERO : deliveryFee;
    }
}

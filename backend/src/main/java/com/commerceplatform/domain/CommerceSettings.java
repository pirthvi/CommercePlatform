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
    public String getCurrency() { return currency; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getFreeDeliveryFrom() { return freeDeliveryFrom; }
    public void configure(String currency, BigDecimal deliveryFee, BigDecimal freeDeliveryFrom) {
        this.currency = currency;
        this.deliveryFee = deliveryFee;
        this.freeDeliveryFrom = freeDeliveryFrom;
    }
    public BigDecimal shippingFor(BigDecimal subtotal) {
        return freeDeliveryFrom != null && subtotal.compareTo(freeDeliveryFrom) >= 0 ? BigDecimal.ZERO : deliveryFee;
    }
}

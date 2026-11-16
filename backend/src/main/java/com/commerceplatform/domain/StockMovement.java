package com.commerceplatform.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_movements", indexes = @Index(name = "idx_stock_product", columnList = "product_id,id"))
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "product_id", nullable = false) private Long productId;
    @Column(nullable = false) private int quantityChange;
    @Column(nullable = false) private int balance;
    @Column(nullable = false, length = 160) private String reason;
    @Column(nullable = false) private Instant createdAt;

    protected StockMovement() {}
    public StockMovement(Product product, int change, String reason) {
        this.productId = product.getId();
        this.quantityChange = change;
        this.balance = product.getStock();
        this.reason = reason;
        this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getQuantityChange() { return quantityChange; }
    public int getBalance() { return balance; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}

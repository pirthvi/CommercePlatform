package com.commerceplatform.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shopping_carts")
public class ShoppingCart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true)
    private CustomerAccount customer;
    @Column(name = "guest_cart_key", unique = true, length = 36)
    private String guestCartKey;
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ShoppingCartItem> items = new ArrayList<>();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected ShoppingCart() {}
    public ShoppingCart(CustomerAccount customer, String guestCartKey) {
        this.customer = customer;
        this.guestCartKey = guestCartKey;
    }

    public Long getId() { return id; }
    public CustomerAccount getCustomer() { return customer; }
    public String getGuestCartKey() { return guestCartKey; }
    public List<ShoppingCartItem> getItems() { return items; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void add(Product product, int quantity) {
        ShoppingCartItem existing = items.stream().filter(item -> item.getProduct().getId().equals(product.getId())).findFirst().orElse(null);
        if (existing == null) items.add(new ShoppingCartItem(this, product, quantity));
        else existing.setQuantity(existing.getQuantity() + quantity);
        touch();
    }
    public void setQuantity(Product product, int quantity) {
        ShoppingCartItem existing = items.stream().filter(item -> item.getProduct().getId().equals(product.getId())).findFirst().orElse(null);
        if (existing == null) items.add(new ShoppingCartItem(this, product, quantity));
        else existing.setQuantity(quantity);
        touch();
    }
    public void remove(Long productId) {
        items.removeIf(item -> item.getProduct().getId().equals(productId));
        touch();
    }
    public void clear() {
        items.clear();
        touch();
    }
    private void touch() { updatedAt = Instant.now(); }
}

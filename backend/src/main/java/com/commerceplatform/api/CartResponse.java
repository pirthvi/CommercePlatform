package com.commerceplatform.api;

import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.ShoppingCart;
import org.hibernate.Hibernate;
import java.util.List;

public record CartResponse(List<Line> items) {
    public static CartResponse from(ShoppingCart cart) {
        return new CartResponse(cart.getItems().stream()
            .map(item -> new Line(Hibernate.unproxy(item.getProduct(), Product.class), item.getQuantity()))
            .toList());
    }
    public record Line(Product product, int quantity) {}
}

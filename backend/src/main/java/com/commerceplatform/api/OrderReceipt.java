package com.commerceplatform.api;

import com.commerceplatform.domain.CustomerOrder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderReceipt(String orderNumber, Instant createdAt, String status, BigDecimal subtotal,
                           BigDecimal shipping, BigDecimal total, String currency, List<ReceiptItem> items) {
    public record ReceiptItem(Long productId, String productName, Integer quantity, BigDecimal unitPrice) {}
    public static OrderReceipt from(CustomerOrder order) {
        var items = order.getItems().stream().map(item -> new ReceiptItem(item.getProductId(), item.getProductName(), item.getQuantity(), item.getUnitPrice())).toList();
        return new OrderReceipt(order.getOrderNumber(), order.getCreatedAt(), order.getStatus(), order.getSubtotal(), order.getShipping(), order.getTotal(), order.getCurrency(), items);
    }
}

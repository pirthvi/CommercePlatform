package com.commerceplatform.service;

import com.commerceplatform.api.StoreProblem;

import com.commerceplatform.api.OrderReceipt;
import com.commerceplatform.api.OrderRequest;
import com.commerceplatform.domain.CustomerOrder;
import com.commerceplatform.domain.CustomerAccount;
import com.commerceplatform.domain.OrderLine;
import com.commerceplatform.domain.Product;
import com.commerceplatform.repository.CustomerOrderRepository;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.CommerceSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {
    private final CommerceSettingsRepository settings;
    private final InventoryService inventory;
    private final CustomerOrderRepository orders;
    private final ProductRepository products;
    private final ShoppingCartService carts;

    public OrderService(CustomerOrderRepository orders, ProductRepository products, ShoppingCartService carts,
                        InventoryService inventory, CommerceSettingsRepository settings) {
        this.orders = orders;
        this.products = products;
        this.carts = carts;
        this.inventory = inventory;
        this.settings = settings;
    }

    @Transactional
    public OrderReceipt create(OrderRequest request, CustomerAccount customer, String guestCartKey) {
        BigDecimal subtotal = BigDecimal.ZERO;
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (var requested : request.items()) {
            int quantity = quantities.getOrDefault(requested.productId(), 0) + requested.quantity();
            if (quantity > 30) throw new StoreProblem(HttpStatus.BAD_REQUEST, "pleaseLimitEachProductTo30UnitsPer");
            quantities.put(requested.productId(), quantity);
        }
        Map<Long, Product> selectedProducts = new LinkedHashMap<>();
        // Acquire stock locks in the same order for checkouts containing multiple products.
        for (var entry : new java.util.TreeMap<>(quantities).entrySet()) {
            Product product = products.findByIdForUpdate(entry.getKey()).orElseThrow(() -> new StoreProblem(HttpStatus.NOT_FOUND, "aProductInYourCartIsNoLonger"));
            if (!product.isActive()) throw new StoreProblem(HttpStatus.CONFLICT, "productUnavailable", java.util.Map.of("name", product.getName()));
            int quantity = entry.getValue();
            if (product.getStock() < quantity) throw new StoreProblem(HttpStatus.CONFLICT, "stockAvailable", java.util.Map.of("count", product.getStock(), "name", product.getName()));
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);
            selectedProducts.put(product.getId(), product);
        }
        var shop = settings.findById(1L).orElseThrow();
        BigDecimal shipping = shop.shippingFor(subtotal);
        var order = new CustomerOrder(UUID.randomUUID().toString(), request.customerName(), request.email(), request.phone(), request.address(), request.city(), request.postalCode(), subtotal, shipping, subtotal.add(shipping), customer);
        order.setCurrency(shop.getCurrency());
        for (var entry : quantities.entrySet()) {
            Product product = selectedProducts.get(entry.getKey());
            order.addItem(new OrderLine(product.getId(), product.getName(), entry.getValue(), product.getPrice()));
            inventory.change(product, -entry.getValue(), "order/" + order.getOrderNumber());
        }
        CustomerOrder saved = orders.save(order);
        carts.clearForOrder(customer, guestCartKey);
        return OrderReceipt.from(saved);
    }
}

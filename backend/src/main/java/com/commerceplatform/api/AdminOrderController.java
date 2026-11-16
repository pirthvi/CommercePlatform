package com.commerceplatform.api;

import com.commerceplatform.domain.CustomerOrder;
import com.commerceplatform.domain.OrderLine;
import com.commerceplatform.service.InventoryService;
import java.util.Comparator;
import com.commerceplatform.repository.CustomerOrderRepository;
import com.commerceplatform.repository.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final CustomerOrderRepository orders;
    private final ProductRepository products;
    private final InventoryService inventory;
    public AdminOrderController(CustomerOrderRepository orders, ProductRepository products, InventoryService inventory) {
        this.orders = orders;
        this.products = products;
        this.inventory = inventory;
    }
    @GetMapping public List<CustomerOrder> list() { return orders.findAllByOrderByCreatedAtDesc(); }
    @PatchMapping("/{id}/status") @Transactional public CustomerOrder updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdate body) {
        if (!List.of("PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED").contains(body.status())) throw new StoreProblem(HttpStatus.BAD_REQUEST, "unknownOrderStatus");
        CustomerOrder order = orders.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        var items = order.getItems().stream().sorted(Comparator.comparing(OrderLine::getProductId)).toList();
        String previousStatus = order.getStatus();
        if (previousStatus.equals(body.status())) return order;
        if (!previousStatus.equals("CANCELLED") && body.status().equals("CANCELLED")) {
            items.forEach(item -> products.findByIdForUpdate(item.getProductId()).ifPresent(product -> inventory.change(product, item.getQuantity(), "cancelled/" + order.getOrderNumber())));
        } else if (previousStatus.equals("CANCELLED")) {
            items.forEach(item -> {
                var product = products.findByIdForUpdate(item.getProductId()).orElseThrow(() -> new StoreProblem(HttpStatus.CONFLICT, "productUnavailable", java.util.Map.of("name", item.getProductName())));
                if (!product.isActive() || product.getStock() < item.getQuantity()) throw new StoreProblem(HttpStatus.CONFLICT, "thereIsNotEnoughStockToReopenThis");
                inventory.change(product, -item.getQuantity(), "reopened/" + order.getOrderNumber());
            });
        }
        order.setStatus(body.status());
        return orders.save(order);
    }
    public record StatusUpdate(@NotBlank @Pattern(regexp = "PROCESSING|SHIPPED|DELIVERED|CANCELLED") String status) {}
}

package com.commerceplatform.api;

import com.commerceplatform.repository.CustomerOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/account/orders")
public class CustomerAccountController {
    private final CustomerOrderRepository orders;
    public CustomerAccountController(CustomerOrderRepository orders) { this.orders = orders; }

    @GetMapping
    public List<OrderReceipt> orderHistory(@AuthenticationPrincipal Jwt jwt) {
        Long customerId = CustomerAuthController.customerId(jwt);
        return orders.findAllByCustomer_IdOrderByCreatedAtDesc(customerId).stream().map(OrderReceipt::from).toList();
    }

    @GetMapping("/{orderNumber}")
    public OrderReceipt order(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNumber) {
        Long customerId = CustomerAuthController.customerId(jwt);
        return orders.findByOrderNumberAndCustomer_Id(orderNumber, customerId).map(OrderReceipt::from)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

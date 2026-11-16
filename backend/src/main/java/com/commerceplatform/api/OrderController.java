package com.commerceplatform.api;

import com.commerceplatform.repository.CustomerOrderRepository;
import com.commerceplatform.repository.CustomerAccountRepository;
import com.commerceplatform.service.OrderService;
import com.commerceplatform.domain.CustomerAccount;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    private final CustomerOrderRepository orders;
    private final CustomerAccountRepository customers;
    public OrderController(OrderService service, CustomerOrderRepository orders, CustomerAccountRepository customers) {
        this.service = service; this.orders = orders; this.customers = customers;
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public OrderReceipt create(@Valid @RequestBody OrderRequest request, Authentication authentication,
                               @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestCartKey) {
        CustomerAccount customer = null;
        if (authentication instanceof JwtAuthenticationToken jwt) {
            Number customerId = jwt.getToken().getClaim("customerId");
            if (customerId != null) customer = customers.findById(customerId.longValue())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer account no longer exists"));
        }
        return service.create(request, customer, guestCartKey);
    }
    @GetMapping("/{orderNumber}") public OrderReceipt receipt(@PathVariable String orderNumber) {
        return orders.findByOrderNumber(orderNumber).map(OrderReceipt::from).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}

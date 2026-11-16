package com.commerceplatform.api;

import com.commerceplatform.service.ShoppingCartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final ShoppingCartService carts;
    public CartController(ShoppingCartService carts) { this.carts = carts; }

    @GetMapping
    public CartResponse get(Authentication authentication, @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestKey) {
        return carts.get(authentication, guestKey);
    }

    @PostMapping("/items") @ResponseStatus(HttpStatus.OK)
    public CartResponse add(Authentication authentication, @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestKey,
                            @Valid @RequestBody CartAddRequest request) {
        return carts.add(authentication, guestKey, request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse quantity(Authentication authentication, @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestKey,
                                 @PathVariable Long productId, @Valid @RequestBody CartQuantityRequest request) {
        return carts.quantity(authentication, guestKey, productId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(Authentication authentication, @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestKey,
                               @PathVariable Long productId) {
        return carts.remove(authentication, guestKey, productId);
    }

    @DeleteMapping
    public CartResponse clear(Authentication authentication, @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestKey) {
        return carts.clear(authentication, guestKey);
    }

    @PostMapping("/merge")
    public CartResponse merge(Authentication authentication, @RequestHeader(name = "X-Guest-Cart-Id", required = false) String guestKey) {
        return carts.merge(authentication, guestKey);
    }
}

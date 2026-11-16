package com.commerceplatform.service;

import com.commerceplatform.api.StoreProblem;

import com.commerceplatform.api.CartAddRequest;
import com.commerceplatform.api.CartQuantityRequest;
import com.commerceplatform.api.CartResponse;
import com.commerceplatform.domain.CustomerAccount;
import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.ShoppingCart;
import com.commerceplatform.repository.CustomerAccountRepository;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.ShoppingCartRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class ShoppingCartService {
    private final ShoppingCartRepository carts;
    private final CustomerAccountRepository customers;
    private final ProductRepository products;

    public ShoppingCartService(ShoppingCartRepository carts, CustomerAccountRepository customers, ProductRepository products) {
        this.carts = carts; this.customers = customers; this.products = products;
    }

    @Transactional
    public CartResponse get(Authentication authentication, String guestKey) {
        ShoppingCart cart = findOrCreate(customer(authentication), guestKey);
        reconcileStock(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse add(Authentication authentication, String guestKey, CartAddRequest request) {
        ShoppingCart cart = findOrCreate(customer(authentication), guestKey);
        Product product = activeProduct(request.productId());
        int current = quantity(cart, product.getId());
        if (current + request.quantity() > 30) throw new StoreProblem(HttpStatus.BAD_REQUEST, "pleaseLimitEachProductTo30PerOrder");
        if (current + request.quantity() > product.getStock()) throw new StoreProblem(HttpStatus.CONFLICT, "stockAvailable", java.util.Map.of("count", product.getStock(), "name", product.getName()));
        cart.add(product, request.quantity());
        return CartResponse.from(carts.save(cart));
    }

    @Transactional
    public CartResponse quantity(Authentication authentication, String guestKey, Long productId, CartQuantityRequest request) {
        ShoppingCart cart = findOrCreate(customer(authentication), guestKey);
        Product product = activeProduct(productId);
        if (request.quantity() > product.getStock()) throw new StoreProblem(HttpStatus.CONFLICT, "stockAvailable", java.util.Map.of("count", product.getStock(), "name", product.getName()));
        if (quantity(cart, productId) == 0) throw new StoreProblem(HttpStatus.NOT_FOUND, "thisProductIsNoLongerInYourCart");
        cart.setQuantity(product, request.quantity());
        return CartResponse.from(carts.save(cart));
    }

    @Transactional
    public CartResponse remove(Authentication authentication, String guestKey, Long productId) {
        ShoppingCart cart = findOrCreate(customer(authentication), guestKey);
        cart.remove(productId);
        return CartResponse.from(carts.save(cart));
    }

    @Transactional
    public CartResponse clear(Authentication authentication, String guestKey) {
        ShoppingCart cart = findOrCreate(customer(authentication), guestKey);
        cart.clear();
        return CartResponse.from(carts.save(cart));
    }

    @Transactional
    public CartResponse merge(Authentication authentication, String guestKey) {
        CustomerAccount customer = customer(authentication);
        if (customer == null) throw new StoreProblem(HttpStatus.UNAUTHORIZED, "signInBeforeMergingAGuestCart");
        String normalizedGuestKey = normalizeGuestKey(guestKey);
        ShoppingCart guestCart = carts.findByGuestCartKey(normalizedGuestKey).orElse(null);
        ShoppingCart customerCart = carts.findByCustomer_Id(customer.getId()).orElseGet(() -> carts.save(new ShoppingCart(customer, null)));
        if (guestCart == null) return CartResponse.from(customerCart);
        for (var item : guestCart.getItems().stream().toList()) {
            Product product = item.getProduct();
            if (!product.isActive() || product.getStock() <= 0) continue;
            int existing = quantity(customerCart, product.getId());
            int merged = Math.min(Math.min(30, product.getStock()), existing + item.getQuantity());
            if (merged > 0) customerCart.setQuantity(product, merged);
        }
        guestCart.clear();
        carts.save(guestCart);
        return CartResponse.from(carts.save(customerCart));
    }

    @Transactional
    public void clearForOrder(CustomerAccount customer, String guestKey) {
        ShoppingCart cart = customer != null
            ? carts.findByCustomer_Id(customer.getId()).orElse(null)
            : guestKey == null ? null : carts.findByGuestCartKey(normalizeGuestKey(guestKey)).orElse(null);
        if (cart != null) { cart.clear(); carts.save(cart); }
    }

    private ShoppingCart findOrCreate(CustomerAccount customer, String guestKey) {
        if (customer != null) return carts.findByCustomer_Id(customer.getId()).orElseGet(() -> carts.save(new ShoppingCart(customer, null)));
        String normalizedGuestKey = normalizeGuestKey(guestKey);
        return carts.findByGuestCartKey(normalizedGuestKey).orElseGet(() -> carts.save(new ShoppingCart(null, normalizedGuestKey)));
    }

    private CustomerAccount customer(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) return null;
        Number customerId = jwt.getToken().getClaim("customerId");
        if (customerId == null) throw new StoreProblem(HttpStatus.UNAUTHORIZED, "customerTokenIsMissingItsAccountId");
        return customers.findById(customerId.longValue()).orElseThrow(() -> new StoreProblem(HttpStatus.UNAUTHORIZED, "customerAccountNoLongerExists"));
    }

    private Product activeProduct(Long id) {
        Product product = products.findById(id).orElseThrow(() -> new StoreProblem(HttpStatus.NOT_FOUND, "productNotFound"));
        if (!product.isActive()) throw new StoreProblem(HttpStatus.CONFLICT, "thisProductIsNoLongerAvailable");
        if (product.getStock() < 1) throw new StoreProblem(HttpStatus.CONFLICT, "thisProductIsSoldOut");
        return product;
    }

    private int quantity(ShoppingCart cart, Long productId) {
        return cart.getItems().stream().filter(item -> item.getProduct().getId().equals(productId)).mapToInt(item -> item.getQuantity()).findFirst().orElse(0);
    }

    private void reconcileStock(ShoppingCart cart) {
        for (var item : cart.getItems().stream().toList()) {
            Product product = item.getProduct();
            if (!product.isActive() || product.getStock() < 1) cart.remove(product.getId());
            else if (item.getQuantity() > product.getStock()) cart.setQuantity(product, product.getStock());
        }
    }

    private String normalizeGuestKey(String guestKey) {
        try { return UUID.fromString(guestKey).toString(); }
        catch (RuntimeException invalid) { throw new StoreProblem(HttpStatus.BAD_REQUEST, "aValidGuestCartIdIsRequired"); }
    }
}

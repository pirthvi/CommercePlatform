package com.commerceplatform.service;

import com.commerceplatform.api.StoreProblem;

import com.commerceplatform.domain.Product;
import com.commerceplatform.domain.StockMovement;
import com.commerceplatform.repository.ProductRepository;
import com.commerceplatform.repository.StockMovementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventoryService {
    private final ProductRepository products;
    private final StockMovementRepository movements;
    public InventoryService(ProductRepository products, StockMovementRepository movements) {
        this.products = products;
        this.movements = movements;
    }
    public Product lock(long id) {
        return products.findByIdForUpdate(id).orElseThrow(() -> new StoreProblem(HttpStatus.NOT_FOUND, "productNotFound"));
    }
    public void recordOpeningBalance(Product product) {
        if (product.getStock() > 0) movements.save(new StockMovement(product, product.getStock(), "opening"));
    }
    public Product adjust(long id, int change, int expected, String reason) {
        Product product = lock(id);
        if (expected != product.getStock()) throw new StoreProblem(HttpStatus.CONFLICT, "stockHasChangedReloadItsCurrentBalanceBefore");
        change(product, change, reason);
        return product;
    }
    public void change(Product product, int delta, String reason) {
        long next = (long) product.getStock() + delta;
        if (next < 0 || next > Integer.MAX_VALUE) throw new StoreProblem(HttpStatus.CONFLICT, "stockBalanceIsOutsideTheSupportedRange");
        if (delta == 0) return;
        product.setStock((int) next);
        movements.save(new StockMovement(product, delta, reason));
    }
}

package com.commerceplatform.repository;

import com.commerceplatform.domain.ShoppingCart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {
    Optional<ShoppingCart> findByCustomer_Id(Long customerId);
    Optional<ShoppingCart> findByGuestCartKey(String guestCartKey);
}

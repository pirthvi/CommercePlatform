package com.commerceplatform.repository;

import com.commerceplatform.domain.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    @EntityGraph(attributePaths = "items") Optional<CustomerOrder> findByOrderNumber(String orderNumber);
    @EntityGraph(attributePaths = "items") Optional<CustomerOrder> findByOrderNumberAndCustomer_Id(String orderNumber, Long customerId);
    @EntityGraph(attributePaths = "items") List<CustomerOrder> findAllByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    @EntityGraph(attributePaths = "items") List<CustomerOrder> findAllByOrderByCreatedAtDesc();
}

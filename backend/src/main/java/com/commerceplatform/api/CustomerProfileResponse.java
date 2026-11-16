package com.commerceplatform.api;

import com.commerceplatform.domain.CustomerAccount;
import java.time.Instant;

public record CustomerProfileResponse(Long id, String fullName, String email, Instant createdAt) {
    public static CustomerProfileResponse from(CustomerAccount customer) {
        return new CustomerProfileResponse(customer.getId(), customer.getFullName(), customer.getEmail(), customer.getCreatedAt());
    }
}

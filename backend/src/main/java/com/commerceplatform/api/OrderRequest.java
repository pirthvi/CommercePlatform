package com.commerceplatform.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record OrderRequest(
    @NotBlank @Size(max = 140) String customerName,
    @NotBlank @Email @Size(max = 180) String email,
    @NotBlank @Size(max = 40) String phone,
    @NotBlank @Size(max = 300) String address,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 24) String postalCode,
    @NotEmpty @Size(max = 30) List<@Valid Line> items
) {
    public record Line(@NotNull Long productId, @NotNull @Min(1) @Max(30) Integer quantity) {}
}

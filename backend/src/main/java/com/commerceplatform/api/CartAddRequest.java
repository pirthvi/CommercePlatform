package com.commerceplatform.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddRequest(@NotNull Long productId, @Min(1) @Max(30) int quantity) {}

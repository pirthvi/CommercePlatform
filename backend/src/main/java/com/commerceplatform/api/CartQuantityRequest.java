package com.commerceplatform.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartQuantityRequest(@Min(1) @Max(30) int quantity) {}

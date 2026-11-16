package com.commerceplatform.api;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use a lowercase URL slug with hyphens") @Size(max = 140) String slug,
    @Positive Long categoryId,
    @Size(max = 80) String category,
    @NotBlank @Size(max = 2000) String description,
    @NotNull @DecimalMin(value = "0.01") BigDecimal price,
    @NotNull @Min(0) Integer stock,
    @Min(0) Integer previousStock,
    @Size(max = 120) String weight,
    @Size(max = 500) String origin,
    @Size(max = 500) String tastingNotes,
    @Size(max = 800) String pairings,
    @Size(max = 500) String ingredients,
    @Size(max = 1200) String highlights,
    @Size(max = 40) String badge,
    boolean featured,
    boolean active,
    boolean replaceGallery
) {}

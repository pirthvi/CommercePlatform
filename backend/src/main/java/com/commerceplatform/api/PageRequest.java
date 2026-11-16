package com.commerceplatform.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PageRequest(
    @NotBlank @Size(max = 140) String title,
    @NotBlank @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "Use a lowercase URL slug with hyphens") @Size(max = 120) String slug,
    @Size(max = 140) String eyebrow,
    @NotBlank @Size(max = 12000) String body,
    @Size(max = 240) String metaDescription,
    @Size(max = 100) String buttonLabel,
    @Size(max = 240) String buttonLink,
    boolean published,
    boolean clearHeroImage
) {}

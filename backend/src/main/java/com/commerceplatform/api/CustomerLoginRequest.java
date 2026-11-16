package com.commerceplatform.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerLoginRequest(
    @NotBlank @Email @Size(max = 180) String email,
    @NotBlank @Size(max = 72) String password
) {}

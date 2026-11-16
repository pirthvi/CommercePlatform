package com.commerceplatform.api;

import java.time.Instant;

public record CustomerAuthResponse(String accessToken, String tokenType, Instant expiresAt, CustomerProfileResponse customer) {}

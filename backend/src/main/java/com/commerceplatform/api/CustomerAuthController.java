package com.commerceplatform.api;

import com.commerceplatform.service.CustomerAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CustomerAuthController {
    private final CustomerAuthService auth;
    public CustomerAuthController(CustomerAuthService auth) { this.auth = auth; }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerAuthResponse register(@Valid @RequestBody CustomerRegistrationRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public CustomerAuthResponse login(@Valid @RequestBody CustomerLoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public CustomerProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.profile(customerId(jwt));
    }

    static Long customerId(Jwt jwt) {
        Number id = jwt.getClaim("customerId");
        return id == null ? null : id.longValue();
    }
}

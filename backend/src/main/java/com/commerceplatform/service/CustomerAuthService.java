package com.commerceplatform.service;

import com.commerceplatform.api.StoreProblem;

import com.commerceplatform.api.CustomerAuthResponse;
import com.commerceplatform.api.CustomerLoginRequest;
import com.commerceplatform.api.CustomerProfileResponse;
import com.commerceplatform.api.CustomerRegistrationRequest;
import com.commerceplatform.domain.CustomerAccount;
import com.commerceplatform.repository.CustomerAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class CustomerAuthService {
    private final CustomerAccountRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;

    public CustomerAuthService(CustomerAccountRepository customers, PasswordEncoder passwordEncoder,
                               AuthenticationManager authenticationManager, JwtTokenService tokens) {
        this.customers = customers;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @Transactional
    public CustomerAuthResponse register(CustomerRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (customers.existsByEmailIgnoreCase(email)) {
            throw new StoreProblem(HttpStatus.CONFLICT, "anAccountWithThisEmailAlreadyExists");
        }
        CustomerAccount customer;
        try {
            customer = customers.save(new CustomerAccount(request.fullName().trim(), email, passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException exception) {
            throw new StoreProblem(HttpStatus.CONFLICT, "anAccountWithThisEmailAlreadyExists");
        }
        return authenticated(customer);
    }

    @Transactional(readOnly = true)
    public CustomerAuthResponse login(CustomerLoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            throw new StoreProblem(HttpStatus.UNAUTHORIZED, "emailOrPasswordIsIncorrect");
        }
        CustomerAccount customer = customers.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new StoreProblem(HttpStatus.UNAUTHORIZED, "emailOrPasswordIsIncorrect"));
        return authenticated(customer);
    }

    @Transactional(readOnly = true)
    public CustomerProfileResponse profile(Long customerId) {
        return customers.findById(customerId).map(CustomerProfileResponse::from)
            .orElseThrow(() -> new StoreProblem(HttpStatus.UNAUTHORIZED, "customerAccountNoLongerExists"));
    }

    private CustomerAuthResponse authenticated(CustomerAccount customer) {
        var token = tokens.issue(customer);
        return new CustomerAuthResponse(token.value(), "Bearer", token.expiresAt(), CustomerProfileResponse.from(customer));
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}

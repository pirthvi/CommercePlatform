package com.commerceplatform.api;

import com.commerceplatform.service.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/session")
public class AdminSessionController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService tokens;

    public AdminSessionController(AuthenticationManager authenticationManager, JwtTokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.tokens = tokens;
    }

    @PostMapping
    public Session login(@Valid @RequestBody AdminLoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        } catch (AuthenticationException exception) {
            throw new StoreProblem(HttpStatus.UNAUTHORIZED, "usernameOrPasswordIsIncorrect");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) throw new StoreProblem(HttpStatus.UNAUTHORIZED, "usernameOrPasswordIsIncorrect");

        var token = tokens.issueAdmin(authentication.getName());
        return new Session(true, token.value(), "Bearer", token.expiresAt());
    }

    @GetMapping
    public SessionStatus session(@AuthenticationPrincipal Jwt jwt) {
        boolean isAdmin = jwt != null && jwt.getClaimAsStringList("roles") != null
            && jwt.getClaimAsStringList("roles").contains("ADMIN");
        return new SessionStatus(isAdmin);
    }

    public record Session(boolean authenticated, String accessToken, String tokenType, Instant expiresAt) {}
    public record SessionStatus(boolean authenticated) {}
}

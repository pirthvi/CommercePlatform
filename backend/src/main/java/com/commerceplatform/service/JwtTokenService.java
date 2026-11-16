package com.commerceplatform.service;

import com.commerceplatform.domain.CustomerAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration lifetime;

    public JwtTokenService(JwtEncoder encoder,
                           @Value("${app.jwt.issuer:commerce-api}") String issuer,
                           @Value("${app.jwt.access-token-minutes:60}") long accessTokenMinutes) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.lifetime = Duration.ofMinutes(accessTokenMinutes);
    }

    public IssuedToken issue(CustomerAccount customer) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        var claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(customer.getEmail())
            .claim("customerId", customer.getId())
            .claim("roles", List.of("CUSTOMER"))
            .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public IssuedToken issueAdmin(String username) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        var claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(username)
            .claim("roles", List.of("ADMIN"))
            .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}

package com.commerceplatform.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

/** A business error with wording managed alongside the rest of the store interface. */
public final class StoreProblem extends ResponseStatusException {
    private final String key;
    private final Map<String, ?> parameters;

    public StoreProblem(HttpStatus status, String key) {
        this(status, key, Map.of());
    }

    public StoreProblem(HttpStatus status, String key, Map<String, ?> parameters) {
        super(status);
        this.key = key;
        this.parameters = Map.copyOf(parameters);
    }

    public String key() { return key; }
    public Map<String, ?> parameters() { return parameters; }
}

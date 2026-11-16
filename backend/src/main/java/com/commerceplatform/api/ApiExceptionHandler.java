package com.commerceplatform.api;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import com.commerceplatform.repository.InterfaceTextRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private final InterfaceTextRepository texts;

    public ApiExceptionHandler(InterfaceTextRepository texts) { this.texts = texts; }

    @ExceptionHandler(StoreProblem.class)
    ProblemDetail businessRule(StoreProblem error, HttpServletRequest request) {
        return message(error.getStatusCode(), error.key(), error.parameters(), request);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail status(ResponseStatusException error) { return error.getBody(); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidInput(MethodArgumentNotValidException error, HttpServletRequest request) {
        var problem = message(HttpStatus.BAD_REQUEST, "invalidInput", Map.of(), request);
        problem.setProperty("fields", error.getBindingResult().getFieldErrors().stream()
            .map(org.springframework.validation.FieldError::getField).distinct().toList());
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail duplicateValue(HttpServletRequest request) {
        return message(HttpStatus.CONFLICT, "conflictingValue", Map.of(), request);
    }

    private ProblemDetail message(org.springframework.http.HttpStatusCode status, String key, Map<String, ?> values, HttpServletRequest request) {
        String scope = request.getServletPath().startsWith("/api/admin/") ? "studio" : "storefront";
        var problem = ProblemDetail.forStatus(status);
        problem.setProperty("code", key);
        texts.findById(scope + ".errors." + key).ifPresent(text -> {
            String detail = text.getWording();
            for (var entry : values.entrySet()) detail = detail.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            problem.setDetail(detail);
        });
        return problem;
    }
}

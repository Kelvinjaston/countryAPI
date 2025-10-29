package com.country.countryAPI.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ApiErrorResponse {
    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private Map<String, String> errors;

    public ApiErrorResponse(HttpStatus status, String message, String path) {
        this(status.value(), status.getReasonPhrase(), message, path, null);
    }
    public ApiErrorResponse(int status, String error, String message, String path, Map<String, String> errors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }
}

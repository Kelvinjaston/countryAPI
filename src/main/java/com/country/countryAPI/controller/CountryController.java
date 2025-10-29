package com.country.countryAPI.controller;

import com.country.countryAPI.dto.ApiErrorResponse;
import com.country.countryAPI.dto.StatusResponse;
import com.country.countryAPI.model.Country;
import com.country.countryAPI.service.CountryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
@Slf4j
public class CountryController {

    private final CountryService countryService;

    @PostMapping("/refresh")
    public ResponseEntity<StatusResponse> refreshData() {
        log.info("CONTROLLER: Received POST request for refresh.");
        StatusResponse status = countryService.refreshCountryData();
        log.info("CONTROLLER: Refresh service call returned successfully.");
        return ResponseEntity.ok(status);
    }
    @GetMapping
    public ResponseEntity<List<Country>> getAllCountries(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false, name = "sort") String sortField
    ) {
        List<Country> countries = countryService.getAllCountries(region, currency, sortField);
        return ResponseEntity.ok(countries);
    }
    @GetMapping("/region/{region}")
    public ResponseEntity<List<Country>> getCountriesByRegion(@PathVariable String region) {
        log.info("Fetching countries from region: {}", region);
        List<Country> countries = countryService.getCountriesByRegion(region);

        if (countries.isEmpty()) {
            log.warn("No countries found in region: {}", region);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(countries);
        }
        return ResponseEntity.ok(countries);
    }
    @GetMapping("/{name}")
    public ResponseEntity<Country> getCountryByName(@PathVariable @NotNull String name) {
        Country country = countryService.getCountryByName(name);
        return ResponseEntity.ok(country);
    }
    @DeleteMapping("/{name}")
    public ResponseEntity<Void> deleteCountryByName(@PathVariable @NotNull String name) {
        countryService.deleteCountryByName(name);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus() {
        StatusResponse status = countryService.getStatus();
        return ResponseEntity.ok(status);
    }
    @GetMapping(value = "/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> getSummaryImage() throws FileNotFoundException {
        Resource resource = countryService.getSummaryImage();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "summary.png");
        return ResponseEntity.ok().headers(headers).body(resource);
    }
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiErrorResponse handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        return ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed for request parameters.")
                .path(request.getRequestURI())
                .errors(errors)
                .build();
    }
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiErrorResponse handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        return ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
    }
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(FileNotFoundException.class)
    public ApiErrorResponse handleFileNotFound(FileNotFoundException ex, HttpServletRequest request) {
        return ApiErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
    }
}

package com.country.countryAPI.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusResponse {
    private long totalCountries;
    private Instant lastRefreshedAt;
}

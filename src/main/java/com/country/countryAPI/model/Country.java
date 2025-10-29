package com.country.countryAPI.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "countries")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "country_seq")
    @SequenceGenerator(
            name = "country_seq",
            sequenceName = "country_seq",
            allocationSize = 50
    )
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String name;

    private String capital;
    private String region;

    @NotNull
    private Long population;
    private String currencyCode;
    private String alpha2Code;

    @Column(precision = 19,scale = 4)
    private BigDecimal exchangeRate;

    @Column(precision = 19,scale = 2)
    private BigDecimal estimatedGdp;

    @Column(name = "flag_url")
    private String flagUrl;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;
}

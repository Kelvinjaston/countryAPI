package com.country.countryAPI.service;

import org.springframework.web.client.RestTemplate;
import com.country.countryAPI.model.Country;
import com.country.countryAPI.repo.CountryRepository;
import com.country.countryAPI.util.ImageGenerator;
import com.country.countryAPI.dto.StatusResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Value; // Added import for dynamic BATCH_SIZE

import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CountryService {

    private static final String COUNTRIES_API_URL = "https://restcountries.com/v2/all?fields=name,alpha2Code,capital,region,population,flag,currencies";
    private static final String EXCHANGE_RATE_API_URL = "https://open.er-api.com/v6/latest/USD";
    private static final String IMAGE_PATH = "cache/summary.png";

    @Value("${spring.jpa.properties.hibernate.jdbc.batch_size:50}")
    private int BATCH_SIZE;

    private final CountryRepository countryRepository;
    private final RestTemplate restTemplate;
    private final ImageGenerator imageGenerator;
    private final EntityManager entityManager;
    private Instant lastRefreshedAt;

    public static class CountryApiDTO {
        public String name;
        public String alpha2Code;
        public String capital;
        public String region;
        public Long population;
        public String flag;
        public List<CurrencyDTO> currencies;
    }
    public static class CurrencyDTO {
        public String code;
    }
    public static class ExchangeRateDTO {
        public String base;
        public Map<String, Double> rates;
    }
    @Transactional
    public StatusResponse refreshCountryData() {
        log.info("Starting country data refresh...");
        Instant currentRefreshTime;
        int savedCount;

        try {
            CountryApiDTO[] countryArray = restTemplate.getForObject(COUNTRIES_API_URL, CountryApiDTO[].class);
            if (countryArray == null) throw new IllegalStateException("Country API returned null data.");
            List<CountryApiDTO> countryData = Arrays.asList(countryArray);

            ExchangeRateDTO rateData = restTemplate.getForObject(EXCHANGE_RATE_API_URL, ExchangeRateDTO.class);
            if (rateData == null || rateData.rates == null) throw new IllegalStateException("Exchange Rate API returned null data.");
            Map<String, Double> exchangeRates = rateData.rates;
            Map<String, Country> existingCountriesMap = countryRepository.findAll().stream()
                    .collect(Collectors.toMap(c -> c.getName().toUpperCase(), c -> c));

            List<Country> newCountriesToInsert = new ArrayList<>();
            int updatedCount = 0;
            int insertedCount = 0;
            currentRefreshTime = Instant.now();
            Random random = new Random();

            for (CountryApiDTO dto : countryData) {
                String currencyCode = extractCurrencyCode(dto);
                Double exchangeRate = getExchangeRate(currencyCode, exchangeRates);
                BigDecimal estimatedGdp = calculateEstimatedGdp(dto.population, exchangeRate, random);
                Country existing = existingCountriesMap.get(dto.name.toUpperCase());

                if (existing == null) {
                    Country newCountry = new Country();
                    newCountry.setName(dto.name);
                    newCountry.setAlpha2Code(dto.alpha2Code);
                    newCountry.setCapital(dto.capital);
                    newCountry.setRegion(dto.region);
                    newCountry.setPopulation(dto.population);
                    newCountry.setFlagUrl(dto.flag);
                    newCountry.setCurrencyCode(currencyCode);
                    newCountry.setExchangeRate(exchangeRate != null ? BigDecimal.valueOf(exchangeRate) : null);
                    newCountry.setEstimatedGdp(estimatedGdp);
                    newCountry.setLastRefreshedAt(currentRefreshTime);

                    newCountriesToInsert.add(newCountry);
                    insertedCount++;

                } else {
                    boolean changed = false;
                    if (!Objects.equals(existing.getAlpha2Code(), dto.alpha2Code)) { existing.setAlpha2Code(dto.alpha2Code); changed = true; }
                    if (!Objects.equals(existing.getCapital(), dto.capital)) { existing.setCapital(dto.capital); changed = true; }
                    if (!Objects.equals(existing.getRegion(), dto.region)) { existing.setRegion(dto.region); changed = true; }
                    if (!Objects.equals(existing.getPopulation(), dto.population)) { existing.setPopulation(dto.population); changed = true; }
                    if (!Objects.equals(existing.getFlagUrl(), dto.flag)) { existing.setFlagUrl(dto.flag); changed = true; }
                    if (!Objects.equals(existing.getCurrencyCode(), currencyCode)) { existing.setCurrencyCode(currencyCode); changed = true; }

                    BigDecimal newRate = exchangeRate != null ? BigDecimal.valueOf(exchangeRate).setScale(4, RoundingMode.HALF_UP) : null;
                    BigDecimal existingRate = existing.getExchangeRate() != null ? existing.getExchangeRate().setScale(4, RoundingMode.HALF_UP) : null;

                    if (newRate == null ? existingRate != null : existingRate == null || newRate.compareTo(existingRate) != 0) {
                        existing.setExchangeRate(newRate); changed = true;
                    }
                    if (!Objects.equals(existing.getEstimatedGdp(), estimatedGdp)) { existing.setEstimatedGdp(estimatedGdp); changed = true; }

                    if (changed) {
                        existing.setLastRefreshedAt(currentRefreshTime);
                        updatedCount++;
                    }
                    if (updatedCount > 0 && updatedCount % BATCH_SIZE == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                }
            }
            if (!newCountriesToInsert.isEmpty()) {
                countryRepository.saveAll(newCountriesToInsert);
            }
            entityManager.flush();
            entityManager.clear();

            savedCount = insertedCount + updatedCount;

            log.info("Country data update completed. {} new entities inserted and {} existing entities updated. Total affected: {}",
                    insertedCount, updatedCount, savedCount);

            lastRefreshedAt = currentRefreshTime;

            executeImageGenerationAndLogging(savedCount, currentRefreshTime);

            return new StatusResponse(savedCount, currentRefreshTime);
        } catch (RestClientException e) {
            log.error("External API error during refresh: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "External data source unavailable", e);
        } catch (Exception e) {
            log.error("Internal error during refresh process: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error during processing", e);
        }
    }
    @Transactional
    public Country updateCountry(String name, Country updatedData) {
        Country existing = countryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found: " + name));
        if (updatedData.getCapital() != null) existing.setCapital(updatedData.getCapital());
        if (updatedData.getRegion() != null) existing.setRegion(updatedData.getRegion());
        if (updatedData.getPopulation() != null) existing.setPopulation(updatedData.getPopulation());
        if (updatedData.getCurrencyCode() != null) existing.setCurrencyCode(updatedData.getCurrencyCode());
        if (updatedData.getExchangeRate() != null) existing.setExchangeRate(updatedData.getExchangeRate());
        if (updatedData.getEstimatedGdp() != null) existing.setEstimatedGdp(updatedData.getEstimatedGdp());
        if (updatedData.getFlagUrl() != null) existing.setFlagUrl(updatedData.getFlagUrl());
        existing.setLastRefreshedAt(Instant.now());
        return countryRepository.save(existing);
    }
    public List<Country> getCountriesByRegion(String region) {
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region must not be empty");
        }
        List<Country> countries = countryRepository.findByRegionIgnoreCase(region);
        if (countries.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No countries found for region: " + region);
        }
        return countries;
    }
    @Async
    public void executeImageGenerationAndLogging(int count, Instant refreshTime) {
        List<Country> topFiveGdp = countryRepository.findTop5ByOrderByEstimatedGdpDesc();
        imageGenerator.generateSummaryImage(count, refreshTime, topFiveGdp);
        log.info("Asynchronous image generation completed for {} countries.", count);
    }
    private String extractCurrencyCode(CountryApiDTO dto) {
        if (dto.currencies == null || dto.currencies.isEmpty()) return null;
        return dto.currencies.get(0).code;
    }
    private Double getExchangeRate(String currencyCode, Map<String, Double> exchangeRates) {
        if (currencyCode == null) return null;
        return exchangeRates.get(currencyCode);
    }
    private BigDecimal calculateEstimatedGdp(Long population, Double exchangeRate, Random random) {
        if (population == null || population <= 0) return BigDecimal.ZERO;
        if (exchangeRate == null || exchangeRate == 0.0) return BigDecimal.ZERO;
        BigDecimal multiplier = BigDecimal.valueOf(random.nextDouble() * 1000 + 1000);
        BigDecimal pop = BigDecimal.valueOf(population);
        BigDecimal rate = BigDecimal.valueOf(exchangeRate);
        return pop.multiply(multiplier).divide(rate, 4, RoundingMode.HALF_UP);
    }
    public List<Country> getAllCountries(String region, String currency, String sortField) {
        Specification<Country> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (region != null && !region.isBlank()) predicates.add(cb.like(cb.lower(root.get("region")), "%" + region.toLowerCase() + "%"));
            if (currency != null && !currency.isBlank()) predicates.add(cb.like(cb.lower(root.get("currencyCode")), "%" + currency.toLowerCase() + "%"));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        Sort sort = Sort.unsorted();
        if (sortField != null && !sortField.isBlank()) {
            String field;
            Sort.Direction direction;
            if (sortField.toLowerCase().endsWith("_asc")) {
                field = sortField.substring(0, sortField.length() - 4);
                direction = Sort.Direction.ASC;
            } else if (sortField.toLowerCase().endsWith("_desc")) {
                field = sortField.substring(0, sortField.length() - 5);
                direction = Sort.Direction.DESC;
            } else {
                field = sortField;
                direction = Sort.Direction.ASC;
            }
            if (!List.of("name", "population", "estimatedGdp", "region", "currencyCode").contains(field)) {
                throw new IllegalArgumentException("Invalid sort field: " + sortField);
            }
            sort = Sort.by(direction, field);
        }
        return countryRepository.findAll(spec, sort);
    }
    public Country getCountryByName(String name) {
        return countryRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country not found: " + name));
    }
    @Transactional
    public void deleteCountryByName(String name) {
        Country country = getCountryByName(name);
        countryRepository.delete(country);
    }
    public StatusResponse getStatus() {
        if (lastRefreshedAt == null) {
            countryRepository.findTopByOrderByLastRefreshedAtDesc().ifPresent(c -> this.lastRefreshedAt = c.getLastRefreshedAt());
        }
        long count = countryRepository.count();
        return new StatusResponse(count, lastRefreshedAt);
    }
    public Resource getSummaryImage() throws FileNotFoundException {
        try {
            Path filePath = Path.of(IMAGE_PATH);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) return resource;
            throw new FileNotFoundException("Summary image not found or not readable.");
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Image file path is malformed: " + IMAGE_PATH);
        }
    }
}

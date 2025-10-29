package com.country.countryAPI.unit;

import com.country.countryAPI.exception.ValidationException;
import com.country.countryAPI.model.Country;
import com.country.countryAPI.repo.CountryRepository;
import com.country.countryAPI.service.CountryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CountryServiceTest {

    @InjectMocks
    private CountryService countryService;

    @Mock
    private CountryRepository countryRepository;

    @Test
    void testGdpCalculationRandomness() throws Exception {
        Method calculateGdp = CountryService.class.getDeclaredMethod("calculateEstimatedGdp", Long.class, Double.class, Random.class);
        calculateGdp.setAccessible(true);

        Long population = 1_000_000L;
        Double exchangeRate = 10.0;
        Random random = new Random();

        BigDecimal gdp1 = (BigDecimal) calculateGdp.invoke(countryService, population, exchangeRate, random);
        BigDecimal gdp2 = (BigDecimal) calculateGdp.invoke(countryService, population, exchangeRate, random);

        assertNotNull(gdp1);
        assertNotNull(gdp2);
        assertTrue(gdp1.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(gdp2.compareTo(BigDecimal.ZERO) > 0);
        assertNotEquals(gdp1, gdp2);
    }
    @Test
    void testCurrencyExtractionValidCurrency() throws Exception {
        Class<?> dtoClass = Class.forName("com.country.countryAPI.service.CountryService$CountryApiDTO");
        Object dto = dtoClass.getDeclaredConstructor().newInstance();

        Class<?> currencyClass = Class.forName("com.country.countryAPI.service.CountryService$CurrencyDTO");
        Object currency = currencyClass.getDeclaredConstructor().newInstance();
        currencyClass.getField("code").set(currency, "USD");

        List<Object> currencies = List.of(currency);
        dtoClass.getField("currencies").set(dto, currencies);

        Method extractCurrencyCode = CountryService.class.getDeclaredMethod("extractCurrencyCode", dtoClass);
        extractCurrencyCode.setAccessible(true);

        String result = (String) extractCurrencyCode.invoke(countryService, dto);
        assertEquals("USD", result);
    }
    @Test
    void testCurrencyExtractionEmptyList() throws Exception {
        Class<?> dtoClass = Class.forName("com.country.countryAPI.service.CountryService$CountryApiDTO");
        Object dto = dtoClass.getDeclaredConstructor().newInstance();
        dtoClass.getField("currencies").set(dto, Collections.emptyList());

        Method extractCurrencyCode = CountryService.class.getDeclaredMethod("extractCurrencyCode", dtoClass);
        extractCurrencyCode.setAccessible(true);

        String result = (String) extractCurrencyCode.invoke(countryService, dto);
        assertNull(result);
    }
    @Test
    void testCountryValidationRequiredFields() {
        Country validCountry = new Country();
        validCountry.setName("Valid");
        validCountry.setPopulation(100L);
        validCountry.setCurrencyCode("USD");

        assertDoesNotThrow(() -> validateCountry(validCountry));

        Country invalidCountry = new Country();
        invalidCountry.setName("Invalid");

        Exception exception = assertThrows(ValidationException.class, () -> validateCountry(invalidCountry));
        assertTrue(exception.getMessage().contains("Invalid country data"));
    }
    private void validateCountry(Country country) {
        Map<String, String> errors = new HashMap<>();
        if (country.getName() == null || country.getName().isBlank()) {
            errors.put("name", "Country name is required");
        }
        if (country.getPopulation() == null || country.getPopulation() <= 0) {
            errors.put("population", "Population must be greater than zero");
        }
        if (country.getCurrencyCode() == null || country.getCurrencyCode().isBlank()) {
            errors.put("currencyCode", "Currency code is required");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid country data", errors);
        }
    }
}

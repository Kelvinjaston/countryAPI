package com.country.countryAPI.integrationtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class CountryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRefreshEndpoint() throws Exception {
        mockMvc.perform(post("/countries/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").exists())
                .andExpect(jsonPath("$.lastRefreshedAt").exists());
    }
    @Test
    void testGetAllCountries() throws Exception {
        mockMvc.perform(get("/countries"))
                .andExpect(status().isOk());
    }
    @Test
    void testGetCountryByNameNotFound() throws Exception {
        mockMvc.perform(get("/countries/NonexistentCountry"))
                .andExpect(status().isNotFound());
    }
    @Test
    void testDeleteCountryByName() throws Exception {
        mockMvc.perform(delete("/countries/Testland"))
                .andExpect(status().isNotFound());
    }
    @Test
    void testStatusEndpoint() throws Exception {
        mockMvc.perform(get("/countries/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").exists())
                .andExpect(jsonPath("$.lastRefreshedAt").exists());
    }
}

package com.country.countryAPI.repo;

import com.country.countryAPI.model.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country,Long>, JpaSpecificationExecutor<Country> {
    Optional<Country> findByNameIgnoreCase(String name);

    void deleteByNameIgnoreCase(String name);
    Optional<Country> findTopByOrderByLastRefreshedAtDesc();

    List<Country> findTop5ByOrderByEstimatedGdpDesc();
    List<Country> findByRegionIgnoreCase(String region);

}

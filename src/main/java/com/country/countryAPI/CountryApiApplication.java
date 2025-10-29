package com.country.countryAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CountryApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(CountryApiApplication.class, args);
	}

}

# Country Currency & Exchange API

A Spring Boot RESTful API that fetches country data and currency exchange rates from external APIs, stores them in a MySQL database, and provides CRUD operations with caching and reporting.

---

##  Features

- Fetches countries and currency exchange rates from external APIs  
- Calculates `estimated_gdp = population × random(1000–2000) ÷ exchange_rate`  
- Stores and updates data in MySQL  
- Supports filters, sorting, and single country retrieval  
- Generates a summary image (`cache/summary.png`) showing:
  - Total number of countries  
  - Top 5 countries by estimated GDP  
  - Timestamp of last refresh  
- Includes unit and integration tests  

---

## Endpoints

| Method | Endpoint | Description |
|---------|-----------|-------------|
| `POST` | `/countries/refresh` | Fetch all countries and exchange rates, then update DB |
| `GET` | `/countries` | Get all countries (supports `?region=`, `?currency=`, `?sort=gdp_desc`) |
| `GET` | `/countries/{name}` | Get a specific country by name |
| `DELETE` | `/countries/{name}` | Delete a country by name |
| `GET` | `/countries/status` | Get total countries and last refresh timestamp |
| `GET` | `/countries/image` | Get summary image (Top 5 GDP countries) |

---

##  Validation Rules

- `name`, `population`, and `currency_code` are **required**
- 
- Return **400 Bad Request** for missing or invalid data  

Example:
```json
{
  "error": "Validation failed",
  "details": {
    "currency_code": "is required"
  }
}
```

External APIs

Countries: https://restcountries.com/v2/all?fields=name,capital,region,population,flag,currencies

Exchange Rates: https://open.er-api.com/v6/latest/USD


Sample Responses

GET /countries?region=Africa

[
  {
    "id": 1,
    "name": "Nigeria",
    "capital": "Abuja",
    "region": "Africa",
    "population": 206139589,
    "currency_code": "NGN",
    "exchange_rate": 1600.23,
    "estimated_gdp": 25767448125.2,
    "flag_url": "https://flagcdn.com/ng.svg",
    "last_refreshed_at": "2025-10-22T18:00:00Z"
  }
]

```
```

GET /countries/status

{
  "total_countries": 250,
  "last_refreshed_at": "2025-10-22T18:00:00Z"
}


Summary Image Example

When /countries/refresh runs, a summary image (cache/summary.png) is generated showing:

Total countries

Top 5 by estimated GDP

Last refreshed timestamp
If no image is found:

{ "error": "Summary image not found" }


Technologies Used

Java 17

Spring Boot 3+

Spring Data JPA (MySQL)

RestTemplate (External API calls)

JUnit 5 & MockMvc (Testing)

H2 Database (Integration Testing)

How to Run Locally

Clone the repository

git clone https://github.com/Kelvinjaston/countryAPI.git


Navigate to the project folder

cd countryAPI

Build and run the app

./mvnw spring-boot:run

or on Windows:
mvnw.cmd spring-boot:run

Open in your browser
http://localhost:8080


Environment Variables

Create a .env file (or use application.properties) and configure:

spring.datasource.url=jdbc:mysql://localhost:3306/countries
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update
server.port=8080


Testing

To run tests:
./mvnw test












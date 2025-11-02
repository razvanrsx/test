# User Service

Spring Boot microservice that provides CRUD APIs for platform users backed by PostgreSQL via Spring Data JPA.

## Running locally

```bash
mvn spring-boot:run
```

Supply datasource properties when running outside Docker Compose:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

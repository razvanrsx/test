# Authorization Service

Java Spring Boot service that issues JWT tokens for authenticated users. Credentials are stored in PostgreSQL and accessed via Spring Data JPA.

## Running locally

```bash
mvn spring-boot:run
```

Provide database credentials through the standard Spring datasource environment variables when running outside Docker:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## Environment variables

- `AUTH_JWT_SECRET` – Secret key used to sign tokens (defaults to a built-in development secret)
- `AUTH_JWT_EXPIRATION_SECONDS` – Expiration in seconds (defaults to 3600)
- `USER_SERVICE_URL` – Base URL for the user service when performing credential lookups

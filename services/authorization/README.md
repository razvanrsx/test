# Authorization Service

Java Spring Boot service that issues JWT tokens for authenticated users. Credentials are loaded from `src/main/resources/data/credentials.json` and can be extended as needed.

## Running locally

```bash
mvn spring-boot:run
```

Environment variables:

- `AUTH_JWT_SECRET` – secret key used to sign tokens (defaults to a built-in development secret)
- `AUTH_JWT_EXPIRATION_SECONDS` – expiration in seconds (defaults to 3600)

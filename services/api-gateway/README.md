# API Gateway Service

Spring Boot gateway that orchestrates calls to the authorization, user, and device services.

## Running locally

```bash
mvn spring-boot:run
```

Environment overrides:

- `AUTH_SERVICE_URL`
- `USER_SERVICE_URL`
- `DEVICE_SERVICE_URL`

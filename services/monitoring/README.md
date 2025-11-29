# Monitoring Service

Consumes device measurements from RabbitMQ and aggregates them into hourly consumption entries stored in PostgreSQL.

## Running locally

```
./mvnw spring-boot:run
```

Configure datasource and RabbitMQ via environment variables:

- `SPRING_DATASOURCE_URL` (default `jdbc:postgresql://localhost:5432/monitoring`)
- `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
- `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD`

## API

- `GET /monitoring/consumption` – list all aggregated hourly consumption rows.
- `GET /monitoring/consumption/device/{deviceId}` – list hourly consumption for a device.
- Swagger UI: `/monitoring/swagger-ui/index.html`

Messages are expected on the durable queue `device.measurements` with JSON payloads shaped as:

```json
{
  "deviceId": 1,
  "consumption": 0.42,
  "timestamp": "2024-01-01T10:00:00Z"
}
```

# Simulator Service

Publishes synthetic device measurements to RabbitMQ so the monitoring service can ingest them.

## Running locally

```
./mvnw spring-boot:run
```

Configuration:

- `SIMULATOR_DEVICE_IDS` (comma-separated list, default `1,2,3`)
- `SIMULATOR_BASE_CONSUMPTION` (base kWh reading, default `0.5`)
- `SIMULATOR_INTERVAL_MS` (publish cadence in milliseconds, default `600000` = 10 minutes)
- RabbitMQ host/port/credentials via the standard `SPRING_RABBITMQ_*` variables.

The simulator emits JSON messages to the durable queue `device.measurements` using the shape:

```json
{
  "deviceId": 1,
  "consumption": 0.61,
  "timestamp": "2024-01-01T10:00:00Z"
}
```

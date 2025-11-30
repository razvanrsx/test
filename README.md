# Energy Management Platform

This repository hosts a Java-based microservice system for an Energy Management Platform. Each service is implemented with Spring Boot and packaged as an individual Docker image that can be orchestrated with Docker Compose or run directly from IntelliJ IDEA.

## Repository structure

```
services/
  authorization/    # Issues JWT tokens backed by a PostgreSQL credential store
  user/             # CRUD operations for platform users persisted in PostgreSQL
  device/           # CRUD operations for energy devices and ownership assignments persisted in PostgreSQL
  monitoring/       # Consumes device measurements from RabbitMQ and aggregates hourly energy consumption in PostgreSQL
  simulator/        # Publishes synthetic device measurements to RabbitMQ
frontend/           # Vite-powered SPA for user and device CRUD flows
```

Supporting files:

- `docker-compose.yml` – Launches the full stack with Traefik and dedicated PostgreSQL instances.
- `docs/` – Architectural notes and ready-to-use HTTP samples for manual testing.

## Getting started

1. **Install prerequisites**
   - Docker and Docker Compose
   - JDK 17+
   - IntelliJ IDEA (Ultimate recommended) with the Docker plugin

2. **Build and start the stack**
   ```bash
   docker compose up --build
   ```
   Exposed endpoints:
   - Traefik entrypoint: `http://localhost:8080`
   - Frontend console (served via Traefik): `http://localhost:8080/`
   - Authorization service (proxied): `http://localhost:8080/api/auth`
   - User service (proxied): `http://localhost:8080/api/users`
   - Device service (proxied): `http://localhost:8080/api/devices`
   - Monitoring service (proxied): `http://localhost:8080/api/monitoring`
   - Swagger UIs: `http://localhost:8080/api/auth/swagger-ui/index.html`, `http://localhost:8080/api/users/swagger-ui/index.html`, `http://localhost:8080/api/devices/swagger-ui/index.html`, `http://localhost:8080/api/monitoring/swagger-ui/index.html`
   - OpenAPI documents: `http://localhost:8080/api/auth/v3/api-docs`, `http://localhost:8080/api/users/v3/api-docs`, `http://localhost:8080/api/devices/v3/api-docs`, `http://localhost:8080/api/monitoring/v3/api-docs`
   - RabbitMQ management UI: `http://localhost:15672` (default credentials `guest` / `guest`)

3. **Obtain a JWT**
   ```bash
   curl -X POST http://localhost:8080/api/auth/token \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}'
   ```
   The response contains a signed JWT token valid for one hour.

4. **Call downstream services through Traefik**
   ```bash
   curl http://localhost:8080/api/users
   curl http://localhost:8080/api/devices
   curl http://localhost:8080/api/devices?userId=1
   ```

## Frontend console

The `frontend/` directory contains a lightweight administrative console that surfaces CRUD operations for users and devices. Data tables stay hidden until you sign in, so the landing experience only shows the sign-in and registration cards.

- **Registration** is open to everyone via the Register card; newly created accounts default to the CLIENT role and can sign in immediately.
- **Admins** can create, edit, and delete users and devices. The Users section and device edit actions remain visible for ADMIN logins.
- **Clients** see a read-only view filtered to their own devices (matched by username/userId) and cannot manage user accounts or modify devices.

Device forms allow selecting an owning user so each device is associated with an account. The devices table surfaces the username alongside device metadata.

Each data grid surfaces its own synchronization label so you can see when users and devices were last refreshed through the Traefik reverse proxy.

The Monitoring card shows hourly consumption aggregates emitted by the simulator and processed by the monitoring service. You can refresh data at any time, filter to a specific device, choose a calendar day, and visualize the per-hour totals as either a line or bar chart in addition to the tabular view. If you just started the stack, the Monitoring section will display a waiting message until the simulator publishes its first batch of readings—records refresh automatically every 15 seconds once you are signed in.

The simulator service emits an initial warm-up reading right after startup and then continues sending JSON measurements to RabbitMQ every 60 seconds. The monitoring service consumes those messages, aggregates them per device hour, and persists the totals into its PostgreSQL database. Aggregated values can be queried through the monitoring REST endpoints or observed directly in the `hourly_consumption` table. If you do not see any chart data, press **Refresh** after a minute to retrieve the first batch of readings.

### Running via Docker Compose

When the stack is started with `docker compose up --build`, the frontend is built into a static bundle and served through an Nginx container behind Traefik. Visit [http://localhost:8080/](http://localhost:8080/) to access the UI.

### Development server

1. Install Node.js 18+.
2. Install dependencies and start Vite:

   ```bash
   npm --prefix frontend install
   npm --prefix frontend run dev
   ```

   The dev server runs on [http://localhost:5173](http://localhost:5173) and proxies API calls under `/api` to Traefik at `http://localhost:8080`. Make sure the backend services are running locally (via Docker Compose or IntelliJ) before using the UI.

### Production build

Set the API endpoint through the `VITE_API_BASE` environment variable and build the static assets:

```bash
VITE_API_BASE=http://localhost:8080/api npm --prefix frontend run build
```

The compiled assets are written to `frontend/dist/` and can be served through any static HTTP server.

## Running in IntelliJ IDEA

Each microservice is a standalone Maven project:

1. Open the repository folder in IntelliJ IDEA.
2. Import Maven projects when prompted. IntelliJ detects all service modules automatically.
3. Use the Maven tool window or create Spring Boot run configurations for the `*ServiceApplication` classes. Update `SPRING_DATASOURCE_*` environment variables in each configuration to point at a running PostgreSQL instance.
4. Optionally configure a Docker Compose run configuration targeting `docker-compose.yml` for full-stack execution.

## Monitoring and messaging

- **Message broker**: RabbitMQ (exposed at `localhost:15672` for the management UI) mediates communication between the simulator producer and the monitoring consumer. Messages land on the durable queue `device.measurements`.
- **Producer**: The `simulator-service` emits measurements on a configurable interval (default 10 minutes) with fields `deviceId`, `consumption`, and `timestamp`.
- **Consumer**: The `monitoring-service` listens to the same queue, truncates timestamps to the hour, and upserts rows into `hourly_consumption` so hourly energy consumption values are persisted.

## Configuration

Environment variables allow customizing service behavior:

- `AUTH_JWT_SECRET` – Secret used to sign JWT tokens.
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` – Override database connectivity per service.
- `USER_SERVICE_URL`, `DEVICE_SERVICE_URL` – Allow the authorization service to call external user/device instances when running outside Docker.

Default credentials and seed data are loaded via `data.sql` in each service and can be adjusted as needed.

## Accessing the databases

Each microservice persists its data in its own PostgreSQL container defined in `docker-compose.yml`. After starting the stack with `docker compose up --build`, connect with the built-in `psql` client from your host by executing the command inside the corresponding container:

```bash
# Authorization credentials database
docker compose exec authorization-db psql -U auth_user -d authorization

# User profile database
docker compose exec user-db psql -U user_user -d users

# Device catalog database
docker compose exec device-db psql -U device_user -d devices

# Monitoring database
docker compose exec monitoring-db psql -U monitor_user -d monitoring
```

The commands above open interactive shells where you can run SQL queries (e.g., `\dt` to list tables or `SELECT * FROM users;`). When you are done, exit the session with `\q`.

If you prefer connecting from an external SQL client, expose the PostgreSQL ports by adding temporary port mappings in `docker-compose.yml` (for example `5432:5432` on `authorization-db`) or by using `docker compose port authorization-db 5432` to discover the ephemeral host port that Docker assigned.

## Assignment 3.1 answers

- **Message broker**: The system uses RabbitMQ as the event-based middleware to decouple producers (simulator) from consumers (monitoring service) and deliver JSON measurement payloads reliably.
- **Monitoring microservice**: The monitoring service consumes measurement messages, aggregates them to hourly totals, and writes the results into its dedicated PostgreSQL schema for later analysis.
- **Data simulator**: A standalone simulator microservice publishes randomised device readings every 10 minutes to the `device.measurements` queue, fulfilling the requirement for an automatic measurement producer.

## Testing the APIs

The `docs/sample-requests.http` file contains sample HTTP requests that can be used with the IntelliJ HTTP client or VS Code REST Client extension. Update hostnames/ports as needed when running the services outside Docker Compose.


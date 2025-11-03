# Energy Management Platform

This repository hosts a Java-based microservice system for an Energy Management Platform. Each service is implemented with Spring Boot and packaged as an individual Docker image that can be orchestrated with Docker Compose or run directly from IntelliJ IDEA.

## Repository structure

```
services/
  authorization/    # Issues JWT tokens backed by a PostgreSQL credential store
  user/             # CRUD operations for platform users persisted in PostgreSQL
  device/           # CRUD operations for energy devices and ownership assignments persisted in PostgreSQL
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

The `frontend/` directory contains a lightweight administrative console that surfaces CRUD operations for users and devices.

Device forms allow selecting an owning user so each device is associated with an account. The devices table surfaces the username alongside device metadata.

### Running via Docker Compose

When the stack is started with `docker compose up --build`, the frontend is built into a static bundle and served through an Nginx container behind Traefik. Visit [http://localhost:8080/](http://localhost:8080/) to access the UI.

### Development server

1. Install Node.js 18+.
2. Install dependencies and start Vite:

   ```bash
   cd frontend
   npm install
   npm run dev
   ```

   The dev server runs on [http://localhost:5173](http://localhost:5173) and proxies API calls under `/api` to Traefik at `http://localhost:8080`. Make sure the backend services are running locally (via Docker Compose or IntelliJ) before using the UI.

### Production build

Set the API endpoint through the `VITE_API_BASE` environment variable and build the static assets:

```bash
cd frontend
VITE_API_BASE=http://localhost:8080/api npm run build
```

The compiled assets are written to `frontend/dist/` and can be served through any static HTTP server.

## Running in IntelliJ IDEA

Each microservice is a standalone Maven project:

1. Open the repository folder in IntelliJ IDEA.
2. Import Maven projects when prompted. IntelliJ detects the three services automatically.
3. Use the Maven tool window or create Spring Boot run configurations for the `*ServiceApplication` classes. Update `SPRING_DATASOURCE_*` environment variables in each configuration to point at a running PostgreSQL instance.
4. Optionally configure a Docker Compose run configuration targeting `docker-compose.yml` for full-stack execution.

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
```

The commands above open interactive shells where you can run SQL queries (e.g., `\dt` to list tables or `SELECT * FROM users;`). When you are done, exit the session with `\q`.

If you prefer connecting from an external SQL client, expose the PostgreSQL ports by adding temporary port mappings in `docker-compose.yml` (for example `5432:5432` on `authorization-db`) or by using `docker compose port authorization-db 5432` to discover the ephemeral host port that Docker assigned.

## Testing the APIs

The `docs/sample-requests.http` file contains sample HTTP requests that can be used with the IntelliJ HTTP client or VS Code REST Client extension. Update hostnames/ports as needed when running the services outside Docker Compose.

## Publishing to GitHub

To upload this project to your own GitHub repository:

1. [Create an empty repository](https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository) in your GitHub account. Skip the option to add a README or license so that the repository stays empty.
2. Add GitHub as a new remote:
   ```bash
   git remote add origin git@github.com:<your-account>/<your-repo>.git
   # or use https if you prefer
   # git remote add origin https://github.com/<your-account>/<your-repo>.git
   ```
3. Push the current branch (named `work`) to GitHub:
   ```bash
   git push -u origin work
   ```
4. If you later create additional branches, push them with `git push -u origin <branch-name>` and open pull requests from the GitHub UI.

These commands require that you have already configured your GitHub credentials (SSH keys or HTTPS token) in your development environment.

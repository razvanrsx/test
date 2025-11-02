# Energy Management Platform

This repository hosts a Java-based microservice system for an Energy Management Platform. Each service is implemented with Spring Boot and packaged as an individual Docker image that can be orchestrated with Docker Compose or run directly from IntelliJ IDEA.

## Repository structure

```
services/
  authorization/    # Issues JWT tokens based on credential store
  user/             # CRUD operations for platform users
  device/           # CRUD operations for energy devices
  api-gateway/      # Lightweight orchestrator forwarding requests to downstream services
```

Supporting files:

- `docker-compose.yml` – Launches the full stack with shared networking.
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
   - API Gateway: `http://localhost:8083`
   - Authorization service: `http://localhost:8080`
   - User service: `http://localhost:8081`
   - Device service: `http://localhost:8082`

3. **Obtain a JWT**
   ```bash
   curl -X POST http://localhost:8080/auth/token \
     -H "Content-Type: application/json" \
     -d '{"username": "admin", "password": "admin123"}'
   ```
   The response contains a signed JWT token valid for one hour.

4. **Call downstream services through the gateway**
   ```bash
   curl http://localhost:8083/gateway/users
   curl http://localhost:8083/gateway/devices
   ```

## Running in IntelliJ IDEA

Each microservice is a standalone Maven project:

1. Open the repository folder in IntelliJ IDEA.
2. Import Maven projects when prompted. IntelliJ detects the four services automatically.
3. Use the Maven tool window or create Spring Boot run configurations for the `*ServiceApplication` classes.
4. Optionally configure a Docker Compose run configuration targeting `docker-compose.yml` for full-stack execution.

## Configuration

Environment variables allow customizing service behavior:

- `AUTH_JWT_SECRET` – Secret used to sign JWT tokens.
- `AUTH_JWT_EXPIRATION_SECONDS` – Token lifetime.
- `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `DEVICE_SERVICE_URL` – Override gateway targets.

Default credentials and seed data are stored as JSON files inside each service under `src/main/resources/data/`.

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

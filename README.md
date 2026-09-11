# Rate Limiting

Java 21 / Spring Boot project for implementing and experimenting with rate-limiting algorithms.

Start with the step-by-step [learning flow](LEARNING_FLOW.md), then read the complete [low-level design](DESIGN.md).

## Included

- Spring MVC for REST endpoints
- Bean Validation for request validation
- Spring Boot Actuator (`/actuator/health`)
- DevTools for local development
- Spring Boot test support

## Run

```bash
cd "LLD Learning/Rate Limiting"
./mvnw spring-boot:run
```

Then visit `http://localhost:8080/actuator/health`.

To exercise the limiter:

```bash
curl -i http://localhost:8080/api/rate-limit/alice
```

## Test

```bash
./mvnw test
```

# Rate Limiting

Java 21 / Spring Boot project for implementing and experimenting with rate-limiting algorithms.

Start with the step-by-step [learning flow](LEARNING_FLOW.md).

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

## Test

```bash
./mvnw test
```

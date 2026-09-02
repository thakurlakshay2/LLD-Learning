# Learning flow: a basic rate limiter

This project starts with a deliberately small **fixed-window, in-memory** rate limiter. It is a learning baseline, not a production-ready solution.

## Goal

Allow each client at most five requests per 60-second window. The limits live in [application.properties](src/main/resources/application.properties) so you can change them without editing Java code.

## Try it

Start the application:

```bash
./mvnw spring-boot:run
```

Make six requests with the same client id:

```bash
for i in {1..6}; do curl -i http://localhost:8080/api/rate-limit/alice; done
```

The first five responses are `200 OK`. The sixth is `429 Too Many Requests`. Try another id, such as `bob`, to see that every client has an independent counter.

## Request flow

```text
HTTP request
    |
    v
RateLimitController
    |
    v
FixedWindowRateLimiter
    |
    +-- Load (or create) the client's counter
    +-- Reset it if its 60-second window ended
    +-- Under the limit? increment and allow
    +-- At the limit? reject with a retry time
    |
    v
HTTP 200 or HTTP 429
```

## Read the code in this order

1. [RateLimitController.java](src/main/java/com/lakshay/lld/ratelimiting/RateLimitController.java) — the HTTP entry point and response headers.
2. [RateLimitResult.java](src/main/java/com/lakshay/lld/ratelimiting/RateLimitResult.java) — the small result object returned to callers.
3. [FixedWindowRateLimiter.java](src/main/java/com/lakshay/lld/ratelimiting/FixedWindowRateLimiter.java) — the algorithm and per-client state.

## What this teaches

- A **key** identifies the thing being limited: here, `clientId` from the URL.
- A **counter** tracks requests for that key.
- A **window** defines when the counter resets.
- `ConcurrentHashMap` allows counters for multiple clients to be held safely in memory.
- Synchronizing a single counter prevents two simultaneous requests from both taking the last available slot.

## Known limitations

- State is lost when the application restarts.
- Multiple application instances do not share limits.
- A fixed window can allow a burst around a window boundary.
- Old client counters are not evicted yet.
- A real system should identify a client from authentication or an API key—not a URL path.

## Next exercises

1. Add unit tests for the limiter.
2. Replace the fixed window with a sliding-window counter.
3. Implement a token-bucket limiter.
4. Store counters in Redis so multiple instances share state.
5. Add a cleanup policy for inactive client ids.

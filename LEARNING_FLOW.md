# Learning flow: a basic rate limiter

This project starts with a deliberately small **fixed-window, in-memory** rate limiter. It is a learning baseline, not a production-ready solution. The point is to understand the pieces before moving to Redis, distributed systems, or a more advanced algorithm.

## Goal

Allow each client at most five requests per 60-second window. The limits live in [application.properties](src/main/resources/application.properties) so you can change them without editing Java code.

| Setting | Default | Meaning |
| --- | ---: | --- |
| `rate-limit.max-requests` | `5` | Requests a single client may make in one window |
| `rate-limit.window-seconds` | `60` | Length of a counting window |

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

### Read one response

For an allowed request, the body looks like this:

```json
{
  "allowed": true,
  "remainingRequests": 4,
  "retryAfterSeconds": 0
}
```

The response also contains these headers:

| Header | Example | Why a client needs it |
| --- | --- | --- |
| `X-RateLimit-Limit` | `5` | The total quota in the current window |
| `X-RateLimit-Remaining` | `4` | The quota left after this request |
| `Retry-After` | `42` | Present only on a `429`; seconds to wait before retrying |

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

## The algorithm, step by step

For every request, `FixedWindowRateLimiter.check(clientId)` does this:

1. Gets the current time and looks up the client's `WindowCounter` in the `ConcurrentHashMap`.
2. Creates a counter when this is that client's first request.
3. Locks only that client's counter. Requests from `alice` do not block `bob`.
4. Checks whether the window has expired. If yes, it sets the count back to zero and begins a new window.
5. Rejects the request when the count has already reached the configured limit.
6. Otherwise, increments the count and returns the remaining quota.

The lookup is approximately **O(1)**. Memory use is **O(number of distinct client ids)**, which is why production systems must remove inactive entries or put them in an external store.

### A concrete example

With a limit of 5 requests per 60 seconds, suppose `alice` starts a window at `12:00:00`:

| Time | Request count before | Result | Remaining |
| --- | ---: | --- | ---: |
| 12:00:03 | 0 | allowed | 4 |
| 12:00:20 | 1 | allowed | 3 |
| 12:00:55 | 4 | allowed | 0 |
| 12:00:56 | 5 | rejected (`429`) | 0 |
| 12:01:00 | 5, but window expired | allowed; counter resets | 4 |

## Why this is called a fixed window

The counter is reset as a whole at the end of a window. This is easy to implement, but it has a boundary problem: a client can send five requests just before a reset and five more just after it. Ten requests may therefore happen in a few seconds even though the nominal rate is five per minute. Later exercises replace this with a sliding window or token bucket to smooth that burst.

## Known limitations

- State is lost when the application restarts.
- Multiple application instances do not share limits.
- A fixed window can allow a burst around a window boundary.
- Old client counters are not evicted yet.
- A real system should identify a client from authentication or an API key—not a URL path.

## Next exercises

1. Add a test for window expiry by injecting a `Clock` rather than calling `System.currentTimeMillis()` directly.
2. Replace the fixed window with a sliding-window counter.
3. Implement a token-bucket limiter.
4. Store counters in Redis so multiple instances share state.
5. Add a cleanup policy for inactive client ids.
6. Identify clients from an API key or authenticated principal instead of a URL path.

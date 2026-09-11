# Rate Limiter — Low-Level Design

## 1. Problem statement

Protect an API from a client making too many requests in a short period. This implementation allows **N requests per client per fixed time window** and returns a decision that the HTTP layer turns into `200 OK` or `429 Too Many Requests`.

This repository intentionally implements one small algorithm well. It is designed so a developer can add another algorithm or a distributed store without rewriting the API layer.

## 2. Scope

### Included

- Per-client limits
- Fixed-window algorithm
- Thread-safe in-memory state
- Configurable limit and window duration
- HTTP headers and JSON response for callers
- Deterministic unit tests through an injected `Clock`

### Out of scope for this version

- Authentication and API-key verification
- Shared state across servers
- Persisting counters across restarts
- Expiring inactive client entries
- Different plans or limits per client

## 3. Components and responsibilities

```text
HTTP client
   |
   v
RateLimitController     translates HTTP <-> domain result
   |
   v
RateLimiter (interface) isolates callers from the algorithm
   |
   v
FixedWindowRateLimiter  owns the fixed-window decision and state
   |
   +--> ConcurrentHashMap<clientId, WindowCounter>

RateLimitProperties     supplies validated runtime configuration
Clock                    supplies time; replaceable in tests
```

| Component | Responsibility | Must not know about |
| --- | --- | --- |
| `RateLimitController` | HTTP status, headers, request path | Counter implementation |
| `RateLimiter` | Stable contract for a limit decision | Spring MVC |
| `FixedWindowRateLimiter` | Atomic per-client counting | HTTP response details |
| `RateLimitProperties` | Validated configuration | Algorithm state |
| `Clock` | Current time | Business rules |

This separation is deliberate. A `TokenBucketRateLimiter` or `RedisRateLimiter` can implement `RateLimiter`; the controller remains unchanged.

## 4. Data model

```text
clientId -> WindowCounter(windowStart, requestCount)
```

`windowStart` is the Unix timestamp at the start of a globally aligned window. With a 60-second window, `12:00:05` belongs to the window starting at `12:00:00`; `12:01:00` starts a new window.

The limiter returns `RateLimitResult`:

| Field | Meaning |
| --- | --- |
| `allowed` | Whether this request may continue |
| `limit` | Maximum requests in this window |
| `remainingRequests` | Requests left after this decision |
| `retryAfterSeconds` | Wait time for a rejected request; zero when allowed |
| `resetAt` | ISO-8601 timestamp of the next window boundary |

## 5. Algorithm

For client key `K`, maximum limit `L`, and window length `W`:

1. Read `now` from `Clock`.
2. Find the start of the current window: `floor(now / W) * W`.
3. Atomically run `ConcurrentHashMap.compute(K, ...)`.
4. If the stored counter belongs to an older window, replace it with a zero-count counter for the current window.
5. If the count is already `L`, deny the request and return a retry duration.
6. Otherwise, increase the count and allow the request.

### Concurrency guarantee

`ConcurrentHashMap.compute` runs atomically for a single key. Two requests for the same client cannot both consume the final available slot. Requests for different clients can proceed concurrently. The map is not locked globally.

### Complexity

- Time per request: expected **O(1)**
- Memory: **O(number of distinct client ids)**

## 6. HTTP contract

```text
GET /api/rate-limit/{clientId}
```

Example:

```bash
curl -i http://localhost:8080/api/rate-limit/alice
```

Allowed response:

```http
HTTP/1.1 200 OK
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 4
```

Rejected response:

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
Retry-After: 37
```

In a production API, derive `clientId` from an authenticated principal or API key. Do not trust a client-provided path variable as an identity boundary.

## 7. Trade-offs and evolution

| Need | Why the current design is insufficient | Recommended next step |
| --- | --- | --- |
| Smoother traffic | Fixed windows burst at boundaries | Sliding-window counter or token bucket |
| Multiple service instances | Local maps are independent | Redis + atomic Lua script or a dedicated rate-limit service |
| Restarts | Memory is erased | External counter store |
| Memory control | Idle client keys remain in the map | TTL cache or periodic eviction |
| Different customer plans | One global config applies to all | Resolve a policy per authenticated client |
| Observability | No metrics or audit events | Micrometer counters, logs, and dashboards |

## 8. Testing strategy

The unit tests cover accepting up to the quota, isolation between client ids, resetting at a fixed-window boundary, and concurrent requests for one client. They use a fixed or mutable `Clock`; no test sleeps or depends on wall-clock time. A MockMvc integration test verifies the `200`/`429` HTTP behavior and rate-limit headers.

When adding a new implementation, keep the `RateLimiter` contract tests and add tests for:

- concurrent requests for one client;
- invalid configuration values;
- `Retry-After` rounding;
- eviction/TTL behavior, if state management is added;
- integration behavior for `200`, `429`, and headers.

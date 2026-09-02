package com.lakshay.lld.ratelimiting;

/**
 * The outcome of one request checked by the rate limiter.
 */
public record RateLimitResult(boolean allowed, int remainingRequests, long retryAfterSeconds) {
}

package com.lakshay.lld.ratelimiting;

import java.time.Instant;

/** The decision returned after a request is checked against a rate limit. */
public record RateLimitResult(
		boolean allowed,
		int limit,
		int remainingRequests,
		long retryAfterSeconds,
		Instant resetAt) {
}

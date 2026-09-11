package com.lakshay.lld.ratelimiting;

/**
 * Contract for a component that decides whether a request may proceed.
 *
 * <p>Keeping this contract separate from an algorithm lets the application
 * switch from a fixed window to a token bucket or a Redis-backed implementation
 * without changing the web layer.</p>
 */
public interface RateLimiter {

	RateLimitResult check(String clientId);

	int limit();
}

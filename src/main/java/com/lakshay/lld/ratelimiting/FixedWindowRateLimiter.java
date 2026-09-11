package com.lakshay.lld.ratelimiting;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * A small, in-memory fixed-window rate limiter.
 *
 * Each client receives {@code maxRequests} requests during a globally aligned
 * window of {@code windowSeconds}. For example, a 60-second policy resets on
 * every minute boundary.
 */
@Service
public class FixedWindowRateLimiter implements RateLimiter {

	private final int maxRequests;
	private final long windowSeconds;
	private final Clock clock;
	private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

	@Autowired
	public FixedWindowRateLimiter(RateLimitProperties properties, Clock clock) {
		this(properties.maxRequests(), properties.windowSeconds(), clock);
	}

	FixedWindowRateLimiter(int maxRequests, long windowSeconds, Clock clock) {
		this.maxRequests = maxRequests;
		this.windowSeconds = windowSeconds;
		this.clock = clock;
	}

	@Override
	public RateLimitResult check(String clientId) {
		Objects.requireNonNull(clientId, "clientId must not be null");
		if (clientId.isBlank()) {
			throw new IllegalArgumentException("clientId must not be blank");
		}

		Instant now = clock.instant();
		long nowEpochSecond = now.getEpochSecond();
		long windowStart = Math.floorDiv(nowEpochSecond, windowSeconds) * windowSeconds;
		long resetEpochSecond = windowStart + windowSeconds;
		Instant resetAt = Instant.ofEpochSecond(resetEpochSecond);
		AtomicReference<RateLimitResult> result = new AtomicReference<>();

		// ConcurrentHashMap.compute is atomic for one key. Requests for different
		// client ids can still run concurrently, while the same client cannot exceed
		// the configured limit through a race condition.
		counters.compute(clientId, (ignored, existingCounter) -> {
			WindowCounter activeCounter = existingCounter != null && existingCounter.windowStart() == windowStart
					? existingCounter
					: new WindowCounter(windowStart, 0);

			if (activeCounter.requestCount() >= maxRequests) {
				long retryAfterSeconds = Math.max(1, resetEpochSecond - nowEpochSecond);
				result.set(new RateLimitResult(false, maxRequests, 0, retryAfterSeconds, resetAt));
				return activeCounter;
			}

			WindowCounter updatedCounter = new WindowCounter(windowStart, activeCounter.requestCount() + 1);
			result.set(new RateLimitResult(true, maxRequests, maxRequests - updatedCounter.requestCount(), 0, resetAt));
			return updatedCounter;
		});

		return result.get();
	}

	@Override
	public int limit() {
		return maxRequests;
	}

	private record WindowCounter(long windowStart, int requestCount) {
	}
}

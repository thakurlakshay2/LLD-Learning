package com.lakshay.lld.ratelimiting;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * A small, in-memory fixed-window rate limiter.
 *
 * Each client receives {@code maxRequests} requests during a window of
 * {@code windowSeconds}. A new window starts when the previous one expires.
 */
@Service
public class FixedWindowRateLimiter {

	private final int maxRequests;
	private final long windowDurationMillis;
	private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

	public FixedWindowRateLimiter(
			@Value("${rate-limit.max-requests:5}") int maxRequests,
			@Value("${rate-limit.window-seconds:60}") long windowSeconds) {
		this.maxRequests = maxRequests;
		this.windowDurationMillis = windowSeconds * 1_000;
	}

	public RateLimitResult check(String clientId) {
		long now = System.currentTimeMillis();
		WindowCounter counter = counters.computeIfAbsent(clientId, ignored -> new WindowCounter(now));

		// Synchronizing one client's counter keeps requests for different clients independent.
		synchronized (counter) {
			if (now >= counter.windowStartedAt + windowDurationMillis) {
				counter.windowStartedAt = now;
				counter.requestCount = 0;
			}

			if (counter.requestCount >= maxRequests) {
				long retryAfterMillis = (counter.windowStartedAt + windowDurationMillis) - now;
				return new RateLimitResult(false, 0, Math.max(1, (retryAfterMillis + 999) / 1_000));
			}

			counter.requestCount++;
			return new RateLimitResult(true, maxRequests - counter.requestCount, 0);
		}
	}

	private static final class WindowCounter {
		private long windowStartedAt;
		private int requestCount;

		private WindowCounter(long windowStartedAt) {
			this.windowStartedAt = windowStartedAt;
		}
	}
}

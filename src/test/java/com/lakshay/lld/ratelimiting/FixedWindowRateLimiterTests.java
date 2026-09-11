package com.lakshay.lld.ratelimiting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTests {

	@Test
	void allowsRequestsUntilTheLimitThenRejectsTheNextOne() {
		FixedWindowRateLimiter limiter = limiter(2, 60, Instant.parse("2026-01-01T12:00:05Z"));

		assertTrue(limiter.check("alice").allowed());
		RateLimitResult secondRequest = limiter.check("alice");
		RateLimitResult rejectedRequest = limiter.check("alice");

		assertTrue(secondRequest.allowed());
		assertEquals(0, secondRequest.remainingRequests());
		assertFalse(rejectedRequest.allowed());
		assertTrue(rejectedRequest.retryAfterSeconds() > 0);
	}

	@Test
	void keepsLimitsSeparateForEachClient() {
		FixedWindowRateLimiter limiter = limiter(1, 60, Instant.parse("2026-01-01T12:00:05Z"));

		assertTrue(limiter.check("alice").allowed());
		assertTrue(limiter.check("bob").allowed());
	}

	@Test
	void resetsTheCounterAtTheNextFixedWindowBoundary() {
		MutableClock clock = new MutableClock(Instant.parse("2026-01-01T12:00:59Z"));
		FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 60, clock);

		assertTrue(limiter.check("alice").allowed());
		assertFalse(limiter.check("alice").allowed());

		clock.advance(Duration.ofSeconds(1));

		assertTrue(limiter.check("alice").allowed());
	}

	@Test
	void allowsOnlyTheQuotaWhenRequestsForOneClientArriveConcurrently() throws Exception {
		FixedWindowRateLimiter limiter = limiter(5, 60, Instant.parse("2026-01-01T12:00:05Z"));
		ExecutorService executor = Executors.newFixedThreadPool(10);
		CountDownLatch start = new CountDownLatch(1);

		try {
			List<Future<Boolean>> decisions = new ArrayList<>();
			for (int request = 0; request < 50; request++) {
				decisions.add(executor.submit(() -> {
					start.await();
					return limiter.check("alice").allowed();
				}));
			}

			start.countDown();
			long allowedRequests = 0;
			for (Future<Boolean> decision : decisions) {
				if (decision.get()) {
					allowedRequests++;
				}
			}

			assertEquals(5, allowedRequests);
		} finally {
			executor.shutdownNow();
		}
	}

	private FixedWindowRateLimiter limiter(int limit, long windowSeconds, Instant instant) {
		return new FixedWindowRateLimiter(limit, windowSeconds, Clock.fixed(instant, ZoneOffset.UTC));
	}

	private static final class MutableClock extends Clock {
		private Instant instant;

		private MutableClock(Instant instant) {
			this.instant = instant;
		}

		@Override
		public ZoneOffset getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		private void advance(Duration duration) {
			instant = instant.plus(duration);
		}
	}
}

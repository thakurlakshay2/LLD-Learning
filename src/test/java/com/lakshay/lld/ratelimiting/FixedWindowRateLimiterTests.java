package com.lakshay.lld.ratelimiting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTests {

	@Test
	void allowsRequestsUntilTheLimitThenRejectsTheNextOne() {
		FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, 60);

		assertTrue(limiter.check("alice").allowed());
		RateLimitResult secondRequest = limiter.check("alice");
		RateLimitResult rejectedRequest = limiter.check("alice");

		assertTrue(secondRequest.allowed());
		assertTrue(secondRequest.remainingRequests() == 0);
		assertFalse(rejectedRequest.allowed());
		assertTrue(rejectedRequest.retryAfterSeconds() > 0);
	}

	@Test
	void keepsLimitsSeparateForEachClient() {
		FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 60);

		assertTrue(limiter.check("alice").allowed());
		assertTrue(limiter.check("bob").allowed());
	}
}

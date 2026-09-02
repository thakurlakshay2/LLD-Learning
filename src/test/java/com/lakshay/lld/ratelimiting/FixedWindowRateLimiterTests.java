package com.lakshay.lld.ratelimiting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTests {

	@Test
	void allowsRequestsUntilTheLimitThenRejectsTheNextOne() {
		FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, 60);

		assertTrue(limiter.check("alice").allowed());
		assertTrue(limiter.check("alice").allowed());
		assertFalse(limiter.check("alice").allowed());
	}

	@Test
	void keepsLimitsSeparateForEachClient() {
		FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 60);

		assertTrue(limiter.check("alice").allowed());
		assertTrue(limiter.check("bob").allowed());
	}
}

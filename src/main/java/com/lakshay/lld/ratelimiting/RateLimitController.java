package com.lakshay.lld.ratelimiting;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RateLimitController {

	private final FixedWindowRateLimiter rateLimiter;

	public RateLimitController(FixedWindowRateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	/**
	 * Demo endpoint: every call counts as one request for the supplied client.
	 */
	@GetMapping("/api/rate-limit/{clientId}")
	public ResponseEntity<RateLimitResult> checkRateLimit(@PathVariable String clientId) {
		RateLimitResult result = rateLimiter.check(clientId);
		return ResponseEntity.status(result.allowed() ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS)
				.header("X-RateLimit-Remaining", String.valueOf(result.remainingRequests()))
				.header("Retry-After", String.valueOf(result.retryAfterSeconds()))
				.body(result);
	}
}

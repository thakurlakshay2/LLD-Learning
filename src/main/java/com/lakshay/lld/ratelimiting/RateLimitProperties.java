package com.lakshay.lld.ratelimiting;

import jakarta.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** External configuration for the active rate-limiting policy. */
@Validated
@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
		@Min(value = 1, message = "max-requests must be at least 1") int maxRequests,
		@Min(value = 1, message = "window-seconds must be at least 1") long windowSeconds) {
}

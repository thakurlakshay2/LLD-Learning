package com.lakshay.lld.ratelimiting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		"rate-limit.max-requests=2",
		"rate-limit.window-seconds=60"
})
@AutoConfigureMockMvc
class RateLimitControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsStandardRateLimitInformationAndRejectsRequestsOverTheQuota() throws Exception {
		String clientId = "integration-test-client";

		mockMvc.perform(get("/api/rate-limit/{clientId}", clientId))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Limit", "2"))
				.andExpect(header().string("X-RateLimit-Remaining", "1"))
				.andExpect(jsonPath("$.allowed").value(true));

		mockMvc.perform(get("/api/rate-limit/{clientId}", clientId))
				.andExpect(status().isOk())
				.andExpect(header().string("X-RateLimit-Remaining", "0"));

		mockMvc.perform(get("/api/rate-limit/{clientId}", clientId))
				.andExpect(status().isTooManyRequests())
				.andExpect(header().string("X-RateLimit-Remaining", "0"))
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.allowed").value(false));
	}
}

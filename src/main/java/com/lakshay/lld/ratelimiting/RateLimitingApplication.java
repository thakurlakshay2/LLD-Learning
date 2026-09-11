package com.lakshay.lld.ratelimiting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitingApplication {

	public static void main(String[] args) {
		SpringApplication.run(RateLimitingApplication.class, args);
	}

}

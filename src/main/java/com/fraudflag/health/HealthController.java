package com.fraudflag.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

	@GetMapping("/health")
	public HealthStatus health() {
		return new HealthStatus("UP");
	}

	public record HealthStatus(String status) {
	}
}

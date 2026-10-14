package com.fraudflag.decision;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Loads tau_low/tau_high from Project 2's thresholds.json if it's present, otherwise falls
 * back to the defaults configured in application.yml and logs a warning.
 */
@Service
public class ThresholdsService {

	private static final Logger log = LoggerFactory.getLogger(ThresholdsService.class);

	private final double tauLow;
	private final double tauHigh;

	public ThresholdsService(ThresholdsProperties defaults, ObjectMapper objectMapper) {
		double low = defaults.tauLow();
		double high = defaults.tauHigh();
		Path path = Path.of(defaults.file());

		if (Files.exists(path)) {
			try {
				ThresholdsFile parsed = objectMapper.readValue(path.toFile(), ThresholdsFile.class);
				low = parsed.tauLow();
				high = parsed.tauHigh();
			} catch (IOException e) {
				log.warn(
						"Failed to parse thresholds file {}, falling back to application.yml defaults (tauLow={}, tauHigh={}): {}",
						path, low, high, e.getMessage());
			}
		} else {
			log.warn(
					"Thresholds file {} not found, falling back to application.yml defaults (tauLow={}, tauHigh={})",
					path, low, high);
		}

		this.tauLow = low;
		this.tauHigh = high;
	}

	public double tauLow() {
		return tauLow;
	}

	public double tauHigh() {
		return tauHigh;
	}

	private record ThresholdsFile(double tauLow, double tauHigh) {
	}
}

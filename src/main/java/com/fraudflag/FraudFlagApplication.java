package com.fraudflag;

import com.fraudflag.decision.ThresholdsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ThresholdsProperties.class)
public class FraudFlagApplication {

	public static void main(String[] args) {
		SpringApplication.run(FraudFlagApplication.class, args);
	}
}

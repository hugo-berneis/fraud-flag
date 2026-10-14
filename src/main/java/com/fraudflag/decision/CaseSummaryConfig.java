package com.fraudflag.decision;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AnthropicProperties.class)
public class CaseSummaryConfig {

	private static final Logger log = LoggerFactory.getLogger(CaseSummaryConfig.class);

	@Bean
	public CaseSummaryClient caseSummaryClient(AnthropicProperties properties) {
		if (properties.apiKey() == null || properties.apiKey().isBlank()) {
			log.warn("ANTHROPIC_API_KEY not set, using MockCaseSummaryClient for case summaries");
			return new MockCaseSummaryClient();
		}
		return new AnthropicCaseSummaryClient(properties);
	}
}

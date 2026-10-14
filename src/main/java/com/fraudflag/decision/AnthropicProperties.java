package com.fraudflag.decision;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraudflag.anthropic")
public record AnthropicProperties(String apiKey, String model) {
}

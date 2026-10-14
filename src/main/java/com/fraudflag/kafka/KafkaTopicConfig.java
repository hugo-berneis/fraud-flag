package com.fraudflag.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

	public static final String TRANSACTIONS_RAW_TOPIC = "transactions.raw";

	@Bean
	public NewTopic transactionsRawTopic() {
		return TopicBuilder.name(TRANSACTIONS_RAW_TOPIC)
				.partitions(3)
				.replicas(1)
				.build();
	}
}

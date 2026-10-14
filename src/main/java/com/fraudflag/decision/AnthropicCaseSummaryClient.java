package com.fraudflag.decision;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;

/**
 * The real LLM client: calls the Anthropic Messages API with a small model, using only the
 * stored feature categories -- never raw transaction data. Writes a case summary; never decides.
 */
public class AnthropicCaseSummaryClient implements CaseSummaryClient {

	private final AnthropicClient client;
	private final String model;

	public AnthropicCaseSummaryClient(AnthropicProperties properties) {
		this.client = AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build();
		this.model = properties.model();
	}

	@Override
	public String summarize(CaseSummaryRequest request) {
		MessageCreateParams params = MessageCreateParams.builder()
				.model(model)
				.maxTokens(200L)
				.addUserMessage(buildPrompt(request))
				.build();

		Message message = client.messages().create(params);

		return message.content().stream()
				.filter(ContentBlock::isText)
				.map(ContentBlock::asText)
				.map(TextBlock::text)
				.findFirst()
				.orElse("");
	}

	private String buildPrompt(CaseSummaryRequest request) {
		return """
				A payment fraud triage system flagged a transaction as uncertain and needs a short case \
				summary for a human reviewer. You only have the computed feature categories below -- no \
				raw transaction data. Write 1-2 plain sentences explaining why this case looks uncertain, \
				to help a reviewer decide whether to approve it or confirm fraud.

				Feature categories: %s
				Rule hits (none means no hard rule fired): %s
				Model's best guess at fraud type: %s
				Model's fraud confidence: %.2f
				"""
				.formatted(
						String.join(", ", request.featureCategories()),
						request.ruleHits().isEmpty() ? "none" : String.join(", ", request.ruleHits()),
						request.fraudTypeGuess() == null ? "unknown" : request.fraudTypeGuess(),
						request.confidence());
	}
}

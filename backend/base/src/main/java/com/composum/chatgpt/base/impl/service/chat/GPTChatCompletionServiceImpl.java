package com.composum.chatgpt.base.impl.service.chat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.impl.RateLimiter;
import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTChatMessage;
import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;

/**
 * Implements the actual access to the ChatGPT chat API.
 *
 * @see "https://platform.openai.com/docs/api-reference/chat/create"
 * @see "https://platform.openai.com/docs/guides/chat"
 */
// FIXME(hps,06.04.23) Handle errors sensibly and retry
// FIXME(hps,06.04.23) more configurability
@Component(service = GPTChatCompletionService.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig.class)
public class GPTChatCompletionServiceImpl implements GPTChatCompletionService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTChatCompletionServiceImpl.class);

    protected static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";

    private String apiKey;
    private String defaultModel;

    private HttpClient httpClient;

    private ObjectMapper mapper;

    private final AtomicInteger requestCounter = new AtomicInteger(0);

    protected RateLimiter limiter;

    @Activate
    public void activate(GPTChatCompletionServiceConfig config) {
        // since it costs a bit of money and there are remote limits, we do limit it somewhat, especially for the case of errors.
        RateLimiter dayLimiter = new RateLimiter(null, 200, 1, TimeUnit.DAYS);
        RateLimiter hourLimiter = new RateLimiter(dayLimiter, 100, 1, TimeUnit.HOURS);
        this.limiter = new RateLimiter(hourLimiter, 20, 1, TimeUnit.MINUTES);
        this.defaultModel = config.defaultModel().trim();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20)).build();
        this.apiKey = config.openAiApiKey().trim();
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @Deactivate
    public void deactivate() {
        httpClient = null;
        apiKey = null;
    }

    @Override
    public String getSingleChatCompletion(GPTChatRequest request) {
        checkEnabled();
        limiter.waitForLimit();
        try {
            List<ChatMessage> messages = new ArrayList<>();
            for (GPTChatMessage message : request.getMessages()) {
                messages.add(new ChatMessage(message.getRole().toString(), message.getContent()));
            }
            ChatCompletionRequest externalRequest = ChatCompletionRequest.builder()
                    .model(defaultModel)
                    .messages(messages)
                    .build();
            String jsonRequest = mapper.writeValueAsString(externalRequest);

            int id = requestCounter.incrementAndGet(); // to easily correlate log messages
            LOG.debug("Sending request {} to GPT: {}", id, jsonRequest);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETION_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            // if response is a 200-ish code, we are good
            // FIXME(hps,06.04.23) if response is a 429, we should retry after the given time
            if (response.statusCode() != 200) {
                LOG.error("Got error response from GPT: {} {}", response.statusCode(), response.body());
                throw new GPTException("Got error response from GPT: " + response.statusCode() + " " + response.body());
            }
            ChatCompletionResult result = mapper.readValue(response.body(), ChatCompletionResult.class);
            // log result.usage
            LOG.debug("Response {} usage {}", id, result.getUsage());
            List<ChatCompletionChoice> choices = result.getChoices();
            if (choices.isEmpty()) {
                LOG.error("Got empty response from GPT: {}", response.body());
                throw new GPTException("Got empty response from GPT: " + response.body());
            }
            LOG.debug("Response {} finish reason: {}", id, choices.get(0).getFinishReason());
            LOG.debug("Response {} from GPT: {}", id, choices.get(0).getMessage());
            return choices.get(0).getMessage().getContent();
        } catch (IOException | InterruptedException e) {
            LOG.error("Error while calling GPT", e);
            throw new GPTException("Error while calling GPT", e);
        }
    }

    private void checkEnabled() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("No API key configured for the GPT chat completion service. Please configure the service.");
        }
    }


    @ObjectClassDefinition(name = "GPT Chat Completion Service",
            description = "Provides rather low level access to the GPT chat completion - use the other services for more specific services.")
    public static @interface GPTChatCompletionServiceConfig {

        @AttributeDefinition(name = "OpenAI API Key from https://platform.openai.com/. If not given, the ")
        String openAiApiKey();

        @AttributeDefinition(name = "Default model to use for the chat completion. The default is gpt-3.5-turbo. Please consider the varying prices https://openai.com/pricing .", defaultValue = "gpt-3.5-turbo")
        String defaultModel();
    }
}

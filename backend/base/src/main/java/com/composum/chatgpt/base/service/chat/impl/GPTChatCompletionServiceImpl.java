package com.composum.chatgpt.base.service.chat.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.framework.BundleContext;
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
import com.composum.chatgpt.base.service.GPTException;
import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTChatMessage;
import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    protected static final Pattern PATTERN_TRY_AGAIN = Pattern.compile("Please try again in (\\d+)s.");

    private String apiKey;
    private String defaultModel;

    private HttpClient httpClient;

    private ObjectMapper mapper;

    private final AtomicLong requestCounter = new AtomicLong(System.currentTimeMillis());

    /**
     * Limiter that maps the financial reasons to limit.
     */
    protected RateLimiter limiter;

    protected volatile long lastGptLimiterCreationTime;

    /**
     * If set, this tells the limits of ChatGPT API itself.
     */
    protected volatile RateLimiter gptLimiter;

    protected volatile GPTChatCompletionServiceConfig config;

    private BundleContext bundleContext;

    private final Map<String, GPTChatMessagesTemplate> templates = new HashMap<>();

    @Activate
    public void activate(GPTChatCompletionServiceConfig config, BundleContext bundleContext) {
        // since it costs a bit of money and there are remote limits, we do limit it somewhat, especially for the case of errors.
        RateLimiter dayLimiter = new RateLimiter(null, 200, 1, TimeUnit.DAYS);
        RateLimiter hourLimiter = new RateLimiter(dayLimiter, 100, 1, TimeUnit.HOURS);
        this.limiter = new RateLimiter(hourLimiter, 20, 1, TimeUnit.MINUTES);
        this.defaultModel = config.defaultModel().trim();
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.connectionTimeout()))
                .build();
        this.config = config;
        this.apiKey = config.openAiApiKey().trim();
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.bundleContext = bundleContext;
        templates.clear(); // bundleContext changed, after all.
    }

    @Deactivate
    public void deactivate() {
        httpClient = null;
        apiKey = null;
        templates.clear();
    }

    @Override
    public String getSingleChatCompletion(GPTChatRequest request) throws GPTException {
        checkEnabled();
        waitForLimit();
        try {
            String jsonRequest = createJsonRequest(request);

            long id = requestCounter.incrementAndGet(); // to easily correlate log messages
            LOG.debug("Sending request {} to GPT: {}", id, jsonRequest);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_COMPLETION_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .timeout(Duration.ofSeconds(config.requestTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response = performCall(httpRequest);

            ChatCompletionResult result = mapper.readValue(response.body(), ChatCompletionResult.class);
            List<ChatCompletionChoice> choices = result.getChoices();
            if (choices.isEmpty()) {
                LOG.error("Got empty response from GPT: {}", response.body());
                throw new GPTException("Got empty response from GPT: " + response.body());
            }
            LOG.debug("Response {} usage {} , finish reason {}", id, result.getUsage(), choices.get(0).getFinishReason());
            LOG.debug("Response {} from GPT: {}", id, choices.get(0).getMessage());
            return choices.get(0).getMessage().getContent();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Error while calling GPT", e);
            throw new GPTException("Error while calling GPT", e);
        }
    }

    protected void waitForLimit() {
        limiter.waitForLimit();
        if (gptLimiter != null && lastGptLimiterCreationTime < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)) {
            LOG.info("Resetting GPT limiter because it is older than a day");
            lastGptLimiterCreationTime = 0;
            gptLimiter = null;
        }
        RateLimiter mygptlimiter = gptLimiter;
        if (mygptlimiter != null) {
            mygptlimiter.waitForLimit();
        }
    }

    protected HttpResponse<String> performCall(HttpRequest httpRequest) throws IOException, InterruptedException {
        HttpResponse<String> response = null;
        long delay = 2000;
        long trynumber = 1;

        while (trynumber < 5) {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) return response;
            if (response.statusCode() != 429) {
                LOG.error("Got unknown response from GPT: {} {}", response.statusCode(), response.body());
                throw new GPTException("Got error response from GPT: " + response.statusCode() + " " + response.body());
            }

            trynumber = trynumber + 1;
            delay = recalculateDelay(response.body(), delay);
            // that normally shouldn't happen except in tests because of rate limiting, so we log a warning
            LOG.warn("Got 429 response from GPT, waiting {} ms: {}", delay, response.body());
            Thread.sleep(delay);
        }

        LOG.error("Got too many 429 responses from GPT, giving up.", response.body());
        throw new GPTException("Got too many 429 responses from GPT: " + response.body());
    }

    /**
     * If the response body contains a string like "Please try again in 20s." (number varies)
     * we return a value of that many seconds, otherwise just use iterative doubling.
     */
    protected long recalculateDelay(String body, long delay) {
        if (body != null) {
            Matcher matcher = PATTERN_TRY_AGAIN.matcher(body);
            if (matcher.find()) {
                if (gptLimiter == null) {
                    gptLimiter = RateLimiter.of(body);
                    lastGptLimiterCreationTime = System.currentTimeMillis();
                }
                return Long.parseLong(matcher.group(1)) * 1000;
            }
        }
        return delay * 2;
    }

    protected String createJsonRequest(GPTChatRequest request) throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        for (GPTChatMessage message : request.getMessages()) {
            messages.add(new ChatMessage(message.getRole().toString(), message.getContent()));
        }
        ChatCompletionRequest externalRequest = ChatCompletionRequest.builder()
                .model(defaultModel)
                .messages(messages)
                .maxTokens(request.getMaxTokens())
                .build();
        String jsonRequest = mapper.writeValueAsString(externalRequest);
        return jsonRequest;
    }

    protected void checkEnabled() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("No API key configured for the GPT chat completion service. Please configure the service.");
        }
    }

    @Nonnull
    @Override
    public GPTChatMessagesTemplate getTemplate(@Nonnull String templateName) throws GPTException {
        GPTChatMessagesTemplate result = templates.get(templateName);
        if (result == null) {
            try {
                // first try to access the normal classloader way - works in tests and possibly somewhere else.
                result = new GPTChatMessagesTemplate(GPTChatCompletionServiceImpl.class.getClassLoader(), templateName);
            } catch (GPTException e) {
                result = new GPTChatMessagesTemplate(bundleContext.getBundle(), templateName);
            }
            templates.put(templateName, result);
        }
        return result;
    }

    @Override
    @Nonnull
    public String shorten(@Nullable String text, int maxwords) {
        if (text == null) {
            return "";
        }
        String[] words = text.split("\\s+");
        if (words.length > maxwords) {
            int middle = words.length / 2;
            int start = maxwords / 2;
            int end = words.length - (maxwords - 1) / 2;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < start; i++) {
                sb.append(words[i]).append(" ");
            }
            sb.append("...");
            for (int i = end; i < words.length; i++) {
                sb.append(" ").append(words[i]);
            }
            return sb.toString();
        } else {
            return text;
        }
    }

    @Override
    public String markdownToHtml(String markdown) {
        // FIXME(hps,20.04.23) not threadsafe, so not shared - if we need it regularily this should be cached.
//        Parser parser = Parser.builder().build();
//        HtmlRenderer renderer = HtmlRenderer.builder().build();
//        Document document = parser.parse(markdown);
//        return renderer.render(document);
        throw new UnsupportedOperationException("Not implemented yet."); // FIXME hps 20.04.23 not implemented
    }

    @Override
    public String htmlToMarkdown(String html) {
        // return FlexmarkHtmlConverter.builder().build().convert(html);
        throw new UnsupportedOperationException("Not implemented yet."); // FIXME hps 20.04.23 not implemented
    }

    @ObjectClassDefinition(name = "GPT Chat Completion Service",
            description = "Provides rather low level access to the GPT chat completion - use the other services for more specific services.")
    public @interface GPTChatCompletionServiceConfig {

        @AttributeDefinition(name = "OpenAI API Key from https://platform.openai.com/. If not given, the ")
        String openAiApiKey();

        @AttributeDefinition(name = "Default model to use for the chat completion. The default is gpt-3.5-turbo. Please consider the varying prices https://openai.com/pricing .", defaultValue = "gpt-3.5-turbo")
        String defaultModel();

        @AttributeDefinition(name = "Connection timeout in seconds", defaultValue = "20")
        int connectionTimeout();

        @AttributeDefinition(name = "Request timeout in seconds", defaultValue = "60")
        int requestTimeout();
    }

}

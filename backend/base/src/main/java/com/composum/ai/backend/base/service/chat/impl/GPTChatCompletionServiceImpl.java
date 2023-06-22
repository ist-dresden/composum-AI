package com.composum.ai.backend.base.service.chat.impl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.eclipse.mylyn.wikitext.markdown.MarkdownLanguage;
import org.eclipse.mylyn.wikitext.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.parser.builder.HtmlDocumentBuilder;
import org.jsoup.internal.StringUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.impl.RateLimiter;
import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.theokanning.openai.completion.chat.ChatCompletionChoice;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;

/**
 * Implements the actual access to the ChatGPT chat API.
 *
 * @see "https://platform.openai.com/docs/api-reference/chat/create"
 * @see "https://platform.openai.com/docs/guides/chat"
 */
// FIXME(hps,06.04.23) check error handling
// FIXME(hps,06.04.23) more configurability
@Component(service = GPTChatCompletionService.class)
@Designate(ocd = GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig.class)
public class GPTChatCompletionServiceImpl implements GPTChatCompletionService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTChatCompletionServiceImpl.class);

    protected static final String CHAT_COMPLETION_URL = "https://api.openai.com/v1/chat/completions";
    protected static final Pattern PATTERN_TRY_AGAIN = Pattern.compile("Please try again in (\\d+)s.");

    /**
     * Environment variable where we take the key from, if not configured directly.
     */
    public static final String OPENAI_API_KEY = "OPENAI_API_KEY";

    /**
     * System property where we take the key from, if not configured directly.
     */
    public static final String OPENAI_API_KEY_SYSPROP = "openai.api.key";

    public static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int DEFAULTVALUE_CONNECTIONTIMEOUT = 20;
    private static final int DEFAULTVALUE_REQUESTTIMEOUT = 60;

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

    protected EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * Tokenizer used for GPT-3.5 and GPT-4.
     */
    protected Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);

    private BundleContext bundleContext;

    private final Map<String, GPTChatMessagesTemplate> templates = new HashMap<>();
    private long requestTimeout;
    private long connectionTimeout;

    @Activate
    public void activate(GPTChatCompletionServiceConfig config, BundleContext bundleContext) {
        LOG.info("Activating GPTChatCompletionService {}", config);
        // since it costs a bit of money and there are remote limits, we do limit it somewhat, especially for the case of errors.
        RateLimiter dayLimiter = new RateLimiter(null, 200, 1, TimeUnit.DAYS);
        RateLimiter hourLimiter = new RateLimiter(dayLimiter, 100, 1, TimeUnit.HOURS);
        this.limiter = new RateLimiter(hourLimiter, 20, 1, TimeUnit.MINUTES);
        this.defaultModel = config != null && config.defaultModel() != null && !config.defaultModel().isBlank() ? config.defaultModel().trim() : DEFAULT_MODEL;
        this.apiKey = null;
        this.requestTimeout = config != null && config.requestTimeout() > 0 ? config.requestTimeout() : DEFAULTVALUE_REQUESTTIMEOUT;
        this.connectionTimeout = config != null && config.connectionTimeout() > 0 ? config.connectionTimeout() : DEFAULTVALUE_CONNECTIONTIMEOUT;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectionTimeout))
                .build();
        if (config == null || !config.disable()) {
            this.apiKey = retrieveOpenAIKey(config);
        } else {
            LOG.info("ChatGPT is disabled.");
        }
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.bundleContext = bundleContext;
        templates.clear(); // bundleContext changed, after all.
        LOG.info("ChatGPT activated: {}", isEnabled());
    }

    private static String retrieveOpenAIKey(@Nullable GPTChatCompletionServiceConfig config) {
        String apiKey = null;
        if (config != null) {
            apiKey = config.openAiApiKey();
            if (apiKey != null && !apiKey.isBlank()) {
                LOG.info("Using OpenAI API key from configuration.");
                return apiKey.trim();
            }
            if (config.openAiApiKeyFile() != null && !config.openAiApiKeyFile().isBlank()) {
                try {
                    apiKey = Files.readString(Paths.get(config.openAiApiKeyFile()));
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read OpenAI API key from file " + config.openAiApiKeyFile(), e);
                }
                if (apiKey != null && !apiKey.isBlank()) {
                    LOG.info("Using OpenAI API key from file {}.", config.openAiApiKeyFile());
                    return apiKey.trim();
                }
            }
        }
        apiKey = System.getenv(OPENAI_API_KEY);
        if (apiKey != null && !apiKey.isBlank()) {
            LOG.info("Using OpenAI API key from environment variable {}.", OPENAI_API_KEY);
            return apiKey.trim();
        }
        apiKey = System.getProperty(OPENAI_API_KEY_SYSPROP);
        if (apiKey != null && !apiKey.isBlank()) {
            LOG.info("Using OpenAI API key from system property {}.", OPENAI_API_KEY_SYSPROP);
            return apiKey.trim();
        }
        return null;
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
        long id = requestCounter.incrementAndGet(); // to easily correlate log messages
        try {
            String jsonRequest = createJsonRequest(request, false);
            LOG.debug("Sending request {} to GPT: {}", id, jsonRequest);

            HttpRequest httpRequest = makeRequest(jsonRequest);
            HttpResponse<String> response = performCall(httpRequest, HttpResponse.BodyHandlers.ofString());

            ChatCompletionResult result = mapper.readValue(response.body(), ChatCompletionResult.class);
            List<ChatCompletionChoice> choices = result.getChoices();
            if (choices.isEmpty()) {
                LOG.error("Got empty response {} from GPT: {}", id, response.body());
                throw new GPTException("Got empty response from GPT: " + response.body());
            }
            LOG.debug("Response {} usage {} , finish reason {}", id, result.getUsage(), choices.get(0).getFinishReason());
            LOG.debug("Response {} from GPT: {}", id, choices.get(0).getMessage());
            return choices.get(0).getMessage().getContent();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Error while call {} to GPT", id, e);
            throw new GPTException("Error while calling GPT", e);
        }
    }

    private HttpRequest makeRequest(String jsonRequest) {
        return HttpRequest.newBuilder()
                .uri(URI.create(CHAT_COMPLETION_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(requestTimeout))
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
    }

    @Override
    public void streamingChatCompletion(GPTChatRequest request, GPTCompletionCallback callback) throws GPTException {
        checkEnabled();
        waitForLimit();
        long id = requestCounter.incrementAndGet(); // to easily correlate log messages
        try {
            String jsonRequest = createJsonRequest(request, true);

            LOG.debug("Sending streaming request {} to GPT: {}", id, jsonRequest);

            HttpRequest httpRequest = makeRequest(jsonRequest);

            performCallAsync(httpRequest, new ResponseLineSubscriber(callback, id), 0, 2000);
            LOG.debug("Response {} from GPT is there and should be streaming", id);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            LOG.error("Error while call {} to GPT", id, e);
            throw new GPTException("Error while calling GPT", e);
        }
    }

    protected class ResponseLineSubscriber implements Flow.Subscriber<String> {
        private final GPTCompletionCallback callback;
        private final long id;
        private volatile Flow.Subscription subscription;

        private volatile boolean cancelled = false;

        public ResponseLineSubscriber(GPTCompletionCallback callback, long id) {
            this.callback = callback;
            this.id = id;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            callback.onSubscribe(
                    new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            subscription.request(n);
                        }

                        @Override
                        public void cancel() {
                            cancelled = true;
                            subscription.cancel();
                        }
                    }
            );
            subscription.request(10000);
        }

        @Override
        public void onNext(String item) {
            if (!cancelled) {
                handleStreamingEvent(callback, id, item);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!cancelled) {
                LOG.error("Error while streaming call {} to GPT", id, throwable);
                callback.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!cancelled) {
                LOG.debug("Response {} from GPT finished", id);
                callback.onComplete();
            }
        }
    }

    /**
     * Handle a single line of the streaming response.
     * <ul>
     *     <li><code>First message e.g.:  {"id":"chatcmpl-xyz","object":"chat.completion.chunk","created":1686890500,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{"role":"assistant"},"index":0,"finish_reason":null}]}</code></li>
     *     <li>Data: <code> gather {"id":"chatcmpl-xyz","object":"chat.completion.chunk","created":1686890500,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{"content":" above"},"index":0,"finish_reason":null}]}</code></li>
     *     <li>End: {"id":"chatcmpl-xyz","object":"chat.completion.chunk","created":1686890500,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{},"index":0,"finish_reason":"stop"}]}</li>
     * </ul>
     */
    protected void handleStreamingEvent(GPTCompletionCallback callback, long id, String line) {
        if (line.startsWith("data:")) {
            line = line.substring(5);
            try {
                if (" [DONE]".equals(line)) {
                    LOG.debug("Response {} from GPT received DONE", id);
                    return;
                }
                ChatCompletionChunk chunk = mapper.readerFor(ChatCompletionChunk.class).readValue(line);
                ChatCompletionChoice choice = chunk.getChoices().get(0);
                String content = choice.getMessage().getContent();
                if (!StringUtil.isBlank(content)) {
                    LOG.debug("Response {} from GPT: {}", id, content);
                    callback.onNext(content);
                }
                GPTFinishReason finishReason = GPTFinishReason.fromChatGPT(choice.getFinishReason());
                if (finishReason != null) {
                    LOG.debug("Response {} from GPT finished with reason {}", id, finishReason);
                    callback.onFinish(finishReason);
                }
            } catch (RuntimeException | IOException e) {
                LOG.error("Id {} Cannot deserialize {}", id, line, e);
                GPTException gptException = new GPTException("Cannot deserialize " + line, e);
                callback.onError(gptException);
                throw gptException;
            }
        } else if (!line.isBlank()) {
            LOG.error("Bug: Got unexpected line from GPT, expecting streaming data: {}", line);
            GPTException gptException = new GPTException("Unexpected line from GPT: " + line);
            callback.onError(gptException);
            throw gptException;
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

    /**
     * Executes a call with retries. It returns a response only if it was HTTP 200, otherwise it's retried or given up.
     *
     * @return the response if it was HTTP 200
     * @throws GPTException         if given up or if there was a non-retryable statuscode
     * @throws IOException          if there was an io error
     * @throws InterruptedException if the thread was interrupted
     */
    // that should be the same as performCallAsync(...,200,0).join(), but I'll replace that when I trust the mechanics there.
    protected <T> HttpResponse<T> performCall(HttpRequest httpRequest, HttpResponse.BodyHandler<T> bodyHandler) throws IOException, InterruptedException {
        HttpResponse<T> response = null;
        long delay = 2000;
        long trynumber = 1;

        while (trynumber < 5) {
            try {
                response = httpClient.send(httpRequest, bodyHandler);

                if (response.statusCode() == 200) return response;
                if (response.statusCode() != 429) {
                    String responsebody = readoutResponse(response);
                    LOG.error("Got unknown response from GPT: {} {}", response.statusCode(), responsebody);
                    throw new GPTException("Got error response from GPT: " + response.statusCode() + " " + responsebody);
                }
            } catch (IOException e) {
                LOG.error("Error while calling GPT", e);
                // do retry.
            }

            trynumber = trynumber + 1;
            String responsebody = readoutResponse(response);
            delay = recalculateDelay(responsebody, delay);
            // that normally shouldn't happen except in tests because of rate limiting, so we log a warning
            LOG.warn("Got error response from GPT, waiting {} ms: {}", delay, responsebody != null ? responsebody : "(no response)");
            Thread.sleep(delay);
        }

        String responsebody = response != null ? readoutResponse(response.body()) : "(no response)";
        LOG.error("Got too many 429 / error responses from GPT, giving up. {}", responsebody);
        throw new GPTException("Got too many 429 / error responses from GPT: " + responsebody);
    }

    /**
     * Executes a call with retries. It returns a response only if it was HTTP 200, otherwise it's retried or given up.
     *
     * @return the response if it was HTTP 200
     * @throws GPTException if given up or if there was a non-retryable statuscode
     */
    protected CompletableFuture<HttpResponse<Void>> performCallAsync(HttpRequest httpRequest, Flow.Subscriber<String> lineSubscriber, int tryNumber, long defaultDelay) {
        if (tryNumber >= 5) {
            LOG.error("Got too many 429 / error responses from GPT, giving up.");
            GPTException gptException = new GPTException("Got too many 429 / error responses from GPT");
            lineSubscriber.onError(gptException);
            throw gptException;
        }

        StringBuilder errorResponseBody = new StringBuilder();

        HttpResponse.BodyHandler<Void> bodyHandler = (responseInfo) -> {
            if (responseInfo.statusCode() == 200) {
                return HttpResponse.BodyHandlers.fromLineSubscriber(lineSubscriber).apply(responseInfo);
            } else {
                return HttpResponse.BodySubscribers.ofByteArrayConsumer(
                        (bytesOpt) -> bytesOpt.ifPresent(bytes ->
                                errorResponseBody.append(new String(bytes, UTF_8))
                        )
                );
            }
        };

        return httpClient.sendAsync(httpRequest, bodyHandler)
                .thenCompose(response -> {
                    if (response.statusCode() == 200) return CompletableFuture.completedFuture(response);
                    if (response.statusCode() != 429) {
                        LOG.error("Got unknown response from GPT: {} {}", response.statusCode(), errorResponseBody);
                        GPTException gptException = new GPTException("Got error response from GPT: " + response.statusCode() + " " + errorResponseBody);
                        lineSubscriber.onError(gptException);
                        throw gptException;
                    }

                    long delay = recalculateDelay(errorResponseBody.toString(), defaultDelay);
                    LOG.warn("Got error response from GPT, waiting {} ms: {}", delay, errorResponseBody != null ? errorResponseBody : "(no response)");

                    return CompletableFuture.completedFuture(null)
                            .thenComposeAsync(v -> performCallAsync(httpRequest, lineSubscriber, tryNumber + 1, delay * 2),
                                    CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS));
                })
                .exceptionally(e -> {
                    lineSubscriber.onError(e);
                    LOG.error("Error while calling GPT", e);
                    GPTException gptException = new GPTException("Error while calling GPT", e);
                    throw gptException;
                });
    }

    String readoutResponse(Object response) {
        if (response == null) {
            return null;
        } else if (response instanceof String) {
            return (String) response;
        } else if (response instanceof Stream) {
            return ((Stream<String>) response).collect(Collectors.joining("\n"));
        } else {
            throw new IllegalArgumentException("Unknown response type: " + response.getClass());
        }
    }

    /**
     * If the response body contains a string like "Please try again in 20s." (number varies)
     * we return a value of that many seconds, otherwise just use iterative doubling.
     */
    protected <T> long recalculateDelay(String responsebody, long delay) {
        if (responsebody != null && !responsebody.isEmpty()) {
            Matcher matcher = PATTERN_TRY_AGAIN.matcher(responsebody);
            if (matcher.find()) {
                if (gptLimiter == null) {
                    gptLimiter = RateLimiter.of(responsebody);
                    lastGptLimiterCreationTime = System.currentTimeMillis();
                }
                return Long.parseLong(matcher.group(1)) * 1000;
            }
        }
        return delay * 2;
    }

    protected String createJsonRequest(GPTChatRequest request, boolean streaming) throws JsonProcessingException {
        List<ChatMessage> messages = new ArrayList<>();
        for (GPTChatMessage message : request.getMessages()) {
            messages.add(new ChatMessage(message.getRole().toString(), message.getContent()));
        }
        ChatCompletionRequest externalRequest = ChatCompletionRequest.builder()
                .model(defaultModel)
                .messages(messages)
                .maxTokens(request.getMaxTokens())
                .stream(streaming ? Boolean.TRUE : null)
                .build();
        String jsonRequest = mapper.writeValueAsString(externalRequest);
        return jsonRequest;
    }

    protected void checkEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("No API key configured for the GPT chat completion service. Please configure the service.");
        }
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
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
            // FIXME(hps,24.05.23) is there a way to do this using tokens? This is a rather wild estimate.
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
        StringWriter writer = new StringWriter();
        HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer, true);
        MarkupParser parser = new MarkupParser(new MarkdownLanguage());
        parser.setBuilder(builder);
        parser.parse(markdown, false);
        return writer.toString();
    }

    @Override
    public int countTokens(@Nullable String text) {
        if (text == null) {
            return 0;
        }
        List<Integer> encoded = this.enc.encodeOrdinary(text);
        return encoded.size();
    }

    @Override
    @Nonnull
    public String htmlToMarkdown(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return new HtmlToMarkdownConverter().convert(html);
    }

    @ObjectClassDefinition(name = "GPT Chat Completion Service",
            description = "Provides rather low level access to the GPT chat completion - use the other services for more specific services.")
    public @interface GPTChatCompletionServiceConfig {

        @AttributeDefinition(name = "Disable the GPT Chat Completion Service", description = "Disable the GPT Chat Completion Service", defaultValue = "false")
        boolean disable();

        @AttributeDefinition(name = "OpenAI API Key from https://platform.openai.com/. If not given, we check the key file, the environment Variable OPENAI_API_KEY, and the system property openai.api.key .")
        String openAiApiKey();

        // alternatively, a key file
        @AttributeDefinition(name = "OpenAI API Key File containing the API key, as an alternative to Open AKI Key configuration and the variants described there.")
        String openAiApiKeyFile();

        @AttributeDefinition(name = "Default model to use for the chat completion. The default is " + DEFAULT_MODEL + ". Please consider the varying prices https://openai.com/pricing .", defaultValue = DEFAULT_MODEL)
        String defaultModel();

        @AttributeDefinition(name = "Connection timeout in seconds", defaultValue = "" + DEFAULTVALUE_CONNECTIONTIMEOUT)
        int connectionTimeout();

        @AttributeDefinition(name = "Request timeout in seconds", defaultValue = "" + DEFAULTVALUE_REQUESTTIMEOUT)
        int requestTimeout();
    }

}

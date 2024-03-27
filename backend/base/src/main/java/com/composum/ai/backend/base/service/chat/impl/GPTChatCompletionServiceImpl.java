package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.reactor.IOReactorConfig;
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
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionChoice;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionMessage;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionMessagePart;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionRequest;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

/**
 * Implements the actual access to the ChatGPT chat API.
 *
 * @see "https://platform.openai.com/docs/api-reference/chat/create"
 * @see "https://platform.openai.com/docs/guides/chat"
 */
@Component(service = GPTChatCompletionService.class)
@Designate(ocd = GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig.class)
public class GPTChatCompletionServiceImpl implements GPTChatCompletionService {

    protected static final Logger LOG = LoggerFactory.getLogger(GPTChatCompletionServiceImpl.class);

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
    public static final String DEFAULT_IMAGE_MODEL = "gpt-4-vision-preview";

    protected static final int DEFAULTVALUE_CONNECTIONTIMEOUT = 20;
    protected static final int DEFAULTVALUE_REQUESTTIMEOUT = 120;

    protected static final int DEFAULTVALUE_REQUESTS_PER_MINUTE = 100;
    protected static final int DEFAULTVALUE_REQUESTS_PER_HOUR = 1000;
    protected static final int DEFAULTVALUE_REQUESTS_PER_DAY = 3000;

    public static final String TRUNCATE_MARKER = " ... (truncated) ... ";
    /**
     * The maximum number of retries.
     */
    public static final int MAXTRIES = 5;

    /**
     * The OpenAI Key for accessing ChatGPT; system default if not given in request.
     */
    protected String apiKey;
    protected String organizationId;
    protected String defaultModel;
    protected String imageModel;
    protected String chatCompletionUrl = CHAT_COMPLETION_URL;

    protected CloseableHttpAsyncClient httpAsyncClient;

    protected static final Gson gson = new GsonBuilder().create();

    protected final AtomicLong requestCounter = new AtomicLong(System.currentTimeMillis());

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

    protected BundleContext bundleContext;

    protected final Map<String, GPTChatMessagesTemplate> templates = new HashMap<>();
    protected int requestTimeout;
    protected int connectionTimeout;
    protected Double temperature;

    protected boolean disabled;

    protected ScheduledExecutorService scheduledExecutorService;

    protected Integer maximumTokensPerRequest;
    protected Integer maximumTokensPerResponse;

    @Activate
    public void activate(GPTChatCompletionServiceConfig config, BundleContext bundleContext) {
        LOG.info("Activating GPTChatCompletionService {}", config);
        // since it costs a bit of money and there are remote limits, we do limit it somewhat, especially for the case of errors.
        int limitPerDay = config != null && config.requestsPerDay() > 0 ? config.requestsPerDay() : DEFAULTVALUE_REQUESTS_PER_DAY;
        RateLimiter dayLimiter = new RateLimiter(null, limitPerDay, 1, TimeUnit.DAYS);
        int limitPerHour = config != null && config.requestsPerHour() > 0 ? config.requestsPerHour() : DEFAULTVALUE_REQUESTS_PER_HOUR;
        RateLimiter hourLimiter = new RateLimiter(dayLimiter, limitPerHour, 1, TimeUnit.HOURS);
        int limitPerMinute = config != null && config.requestsPerMinute() > 0 ? config.requestsPerMinute() : DEFAULTVALUE_REQUESTS_PER_MINUTE;
        this.limiter = new RateLimiter(hourLimiter, limitPerMinute, 1, TimeUnit.MINUTES);
        this.defaultModel = config != null && config.defaultModel() != null && !config.defaultModel().trim().isEmpty() ? config.defaultModel().trim() : DEFAULT_MODEL;
        this.imageModel = config != null && config.imageModel() != null && !config.imageModel().trim().isEmpty() ? config.imageModel().trim() : null;
        this.apiKey = null;
        this.requestTimeout = config != null && config.requestTimeout() > 0 ? config.requestTimeout() : DEFAULTVALUE_REQUESTTIMEOUT;
        this.connectionTimeout = config != null && config.connectionTimeout() > 0 ? config.connectionTimeout() : DEFAULTVALUE_CONNECTIONTIMEOUT;
        this.maximumTokensPerRequest = config != null && config.maximumTokensPerRequest() > 0 ? config.maximumTokensPerRequest() : null;
        this.maximumTokensPerResponse = config != null && config.maximumTokensPerResponse() > 0 ? config.maximumTokensPerResponse() : null;
        try {
            this.temperature = config != null && !StringUtil.isBlank(config.temperature()) ? Double.valueOf(config.temperature()) : null;
        } catch (NumberFormatException e) {
            LOG.error("Cannot parse temperature {}", config.temperature(), e);
            this.temperature = null;
        }
        this.disabled = config != null && config.disabled();
        if (!disabled) {
            this.apiKey = retrieveOpenAIKey(config);
            this.organizationId = config.openAiOrganizationId();
        } else {
            LOG.info("ChatGPT is disabled.");
        }
        if (config.chatCompletionUrl() != null && !config.chatCompletionUrl().trim().isEmpty()) {
            this.chatCompletionUrl = config.chatCompletionUrl().trim();
        }
        if (isEnabled()) {
            PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setDefaultConnectionConfig(ConnectionConfig.custom()
                            .setSocketTimeout(this.requestTimeout, TimeUnit.SECONDS)
                            .setConnectTimeout(this.connectionTimeout, TimeUnit.SECONDS)
                            .build()
                    )
                    .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                    .build();
            this.httpAsyncClient = HttpAsyncClients.custom()
                    .setIOReactorConfig(IOReactorConfig.custom()
                            .setSoTimeout(this.requestTimeout, TimeUnit.SECONDS)
                            .setIoThreadCount(10)
                            .build())
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setResponseTimeout(this.requestTimeout, TimeUnit.SECONDS).build())
                    .build();
            this.httpAsyncClient.start();

            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setDaemon(true);
                return thread;
            });
        } else {
            this.httpAsyncClient = null;
        }
        this.bundleContext = bundleContext;
        templates.clear(); // bundleContext changed, after all.
        LOG.info("ChatGPT activated: {}", isEnabled());
    }

    @Deactivate
    public void deactivate() {
        LOG.info("Deactivating GPTChatCompletionService");
        if (this.httpAsyncClient != null) {
            this.httpAsyncClient.close(CloseMode.IMMEDIATE);
            this.httpAsyncClient = null;
        }
        this.apiKey = null;
        this.defaultModel = null;
        this.imageModel = null;
        this.limiter = null;
        this.gptLimiter = null;
        this.bundleContext = null;
        this.templates.clear();
        this.temperature = null;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
    }

    protected static String retrieveOpenAIKey(@Nullable GPTChatCompletionServiceConfig config) {
        String apiKey = null;
        if (config != null) {
            apiKey = config.openAiApiKey();
            if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("$[secret")) {
                LOG.info("Using OpenAI API key from configuration.");
                return apiKey.trim();
            }
            if (config.openAiApiKeyFile() != null && !config.openAiApiKeyFile().trim().isEmpty()) {
                try {
                    apiKey = new String(Files.readAllBytes(Paths.get(config.openAiApiKeyFile())));
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read OpenAI API key from file " + config.openAiApiKeyFile(), e);
                }
                if (apiKey != null && !apiKey.trim().isEmpty()) {
                    LOG.info("Using OpenAI API key from file {}.", config.openAiApiKeyFile());
                    return apiKey.trim();
                }
            }
        }
        apiKey = System.getenv(OPENAI_API_KEY);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            LOG.info("Using OpenAI API key from environment variable {}.");
            return apiKey.trim();
        }
        apiKey = System.getProperty(OPENAI_API_KEY_SYSPROP);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            LOG.info("Using OpenAI API key from system property {}.");
            return apiKey.trim();
        }
        return null;
    }

    @Override
    public String getSingleChatCompletion(@Nonnull GPTChatRequest request) throws GPTException {
        checkEnabled();
        waitForLimit();
        long id = requestCounter.incrementAndGet(); // to easily correlate log messages
        try {
            String jsonRequest = createJsonRequest(request);
            LOG.debug("Sending request {} to GPT: {}", id, jsonRequest);

            SimpleHttpRequest httpRequest = makeRequest(jsonRequest, request.getConfiguration());
            GPTCompletionCallback.GPTCompletionCollector callback = new GPTCompletionCallback.GPTCompletionCollector();
            CompletableFuture<Void> finished = new CompletableFuture<>();
            performCallAsync(finished, id, httpRequest, callback, 0, 2000);
            finished.get(this.requestTimeout, TimeUnit.SECONDS);
            if (callback.getFinishReason() != GPTFinishReason.STOP) {
                LOG.warn("Response {} from GPT finished with reason {}", id, callback.getFinishReason());
            }
            if (callback.getError() != null) {
                if (callback.getError() instanceof GPTException) {
                    throw (GPTException) callback.getError();
                }
                throw new GPTException("Error while calling GPT", callback.getError());
            }
            return callback.getResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted during call {} to GPT", id, e);
            throw new GPTException("Interrupted during call to GPT", e);
        } catch (IOException e) {
            if (!e.toString().contains("Stream") || !e.toString().contains("cancelled")) {
                LOG.error("IO error while call {} to GPT", id, e);
            }
            throw new GPTException("Error while calling GPT", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof GPTException.GPTContextLengthExceededException) {
                LOG.info("Context length exceeded while call {} to GPT", id);
            } else {
                LOG.error("Execution error while call {} to GPT", id, e);
            }
            if (cause instanceof GPTException || cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new GPTException("Execution Error while calling GPT", e);
        } catch (TimeoutException e) {
            LOG.error("" + e, e);
            throw new GPTException("Timeout while calling GPT", e);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    protected SimpleHttpRequest makeRequest(String jsonRequest, GPTConfiguration gptConfiguration) {
        String actualApiKey = gptConfiguration != null && gptConfiguration.getApiKey() != null && !gptConfiguration.getApiKey().trim().isEmpty() ? gptConfiguration.getApiKey() : this.apiKey;
        String actualOrganizationId = gptConfiguration != null && gptConfiguration.getOrganizationId() != null && !gptConfiguration.getOrganizationId().trim().isEmpty() ? gptConfiguration.getOrganizationId() : this.organizationId;
        SimpleHttpRequest request = new SimpleHttpRequest("POST", chatCompletionUrl);
        request.setBody(jsonRequest, ContentType.APPLICATION_JSON);
        request.addHeader("Authorization", "Bearer " + actualApiKey);
        if (actualOrganizationId != null && !actualOrganizationId.trim().isEmpty()) {
            request.addHeader("OpenAI-Organization", actualOrganizationId);
        }
        return request;
    }

    @Override
    public void streamingChatCompletion(@Nonnull GPTChatRequest request, @Nonnull GPTCompletionCallback callback) throws GPTException {
        checkEnabled();
        waitForLimit();
        long id = requestCounter.incrementAndGet(); // to easily correlate log messages
        try {
            String jsonRequest = createJsonRequest(request);
            callback.setRequest(jsonRequest);

            if (LOG.isDebugEnabled()) {
                // replace data:image/jpeg;base64,{base64_image} with data:image/jpeg;base64, ...
                String shortenedRequest = jsonRequest.replaceAll("data:image/[^;]+;base64,[^\\}]+\\}", "data:image/jpeg;base64,{base64_image}");
                LOG.debug("Sending streaming request {} to GPT: {}", id, shortenedRequest);
            }

            SimpleHttpRequest httpRequest = makeRequest(jsonRequest, request.getConfiguration());
            performCallAsync(new CompletableFuture<>(), id, httpRequest, callback, 0, 2000);
            LOG.debug("Response {} from GPT is there and should be streaming", id);
        } catch (IOException e) {
            LOG.error("Error while call {} to GPT", id, e);
            throw new GPTException("Error while calling GPT", e);
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
            line = line.substring(MAXTRIES);
            try {
                if (" [DONE]".equals(line)) {
                    LOG.debug("Response {} from GPT received DONE", id);
                    return;
                }
                ChatCompletionResponse chunk = gson.fromJson(line, ChatCompletionResponse.class);
                ChatCompletionChoice choice = chunk.getChoices().get(0);
                String content = choice.getDelta().getContent();
                if (content != null && !content.isEmpty()) {
                    LOG.trace("Response {} from GPT: {}", id, content);
                    callback.onNext(content);
                }
                GPTFinishReason finishReason = ChatCompletionResponse.FinishReason.toGPTFinishReason(choice.getFinishReason());
                if (finishReason != null) {
                    LOG.debug("Response {} from GPT finished with reason {}", id, finishReason);
                    callback.onFinish(finishReason);
                }
            } catch (RuntimeException e) {
                LOG.error("Id {} Cannot deserialize {}", id, line, e);
                GPTException gptException = new GPTException("Cannot deserialize " + line, e);
                callback.onError(gptException);
                throw gptException;
            }
        } else if (!line.trim().isEmpty()) {
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
     * Executes a call with retries. The response is written to callback; when it's finished the future is set - either normally or exceptionally if there was an error.
     *
     * @param finished    the future to set when the call is finished
     * @param id          the id of the call, for logging
     * @param httpRequest the request to send
     * @param callback    the callback to write the response to
     * @param tryNumber   the number of the try - if it's {@value #MAXTRIES} , we give up.
     */
    protected void performCallAsync(CompletableFuture<Void> finished, long id, SimpleHttpRequest httpRequest,
                                    GPTCompletionCallback callback, int tryNumber, long defaultDelay) {
        if (tryNumber >= MAXTRIES) {
            LOG.error("Got too many 429 / error responses from GPT, giving up.");
            GPTException gptException = new GPTException("Got too many 429 / error responses from GPT");
            callback.onError(gptException);
            finished.completeExceptionally(gptException);
        }
        CompletableFuture<Void> callFuture = triggerCallAsync(id, httpRequest, callback);
        callFuture.thenAccept(finished::complete)
                .exceptionally(e -> {
                    RetryableException retryable = extractRetryableException(e);
                    if (retryable != null) {
                        long newDelay = recalculateDelay(readoutResponse(e.getMessage()), defaultDelay);
                        LOG.debug("Call {} to GPT failed, retry after {} ms because of {}", id, newDelay, e.toString());
                        performCallAsync(finished, id, httpRequest, callback, tryNumber + 1, newDelay);
                    } else {
                        finished.completeExceptionally(e);
                    }
                    return null;
                });
    }

    protected static RetryableException extractRetryableException(Throwable e) {
        RetryableException retryable = null;
        if (e instanceof RetryableException) {
            retryable = (RetryableException) e;
        } else if (e instanceof CompletionException) {
            CompletionException completionException = (CompletionException) e;
            if (completionException.getCause() instanceof RetryableException) {
                retryable = (RetryableException) completionException.getCause();
            }
        }
        return retryable;
    }

    /**
     * Puts the call into the pipeline; the returned future will be set normally or exceptionally when it's done.
     */
    protected CompletableFuture<Void> triggerCallAsync(long id, SimpleHttpRequest httpRequest, GPTCompletionCallback callback) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        AsyncResponseConsumer<Void> responseConsumer = new StreamDecodingResponseConsumer(callback, result, id);
        httpAsyncClient.execute(SimpleRequestProducer.create(httpRequest), responseConsumer,
                new EnsureResultFutureCallback(result));
        return result;
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

    protected String createJsonRequest(GPTChatRequest request) throws JsonProcessingException {
        List<ChatCompletionMessage> messages = new ArrayList<>();
        for (GPTChatMessage message : request.getMessages()) {
            messages.add(ChatCompletionMessage.make(message));
        }
        for (Iterator<ChatCompletionMessage> messageIterator = messages.iterator(); messageIterator.hasNext(); ) {
            ChatCompletionMessage message = messageIterator.next();
            if (message.isEmpty(null)) {
                LOG.debug("Removing empty message {}", message); // suspicious - likely misusage of the API
                messageIterator.remove();
            }
        }
        if (!messages.isEmpty() && messages.get(messages.size() - 1).getRole() == ChatCompletionRequest.Role.ASSISTANT) {
            LOG.debug("Removing last message because it's an assistant message and that'd be confusing for GPT.");
            messages.remove(messages.size() - 1);
        }
        boolean hasImage = messages.stream().flatMap(m -> m.getContent().stream())
                .anyMatch(m -> m.getType() == ChatCompletionMessagePart.Type.IMAGE_URL);
        if (hasImage && imageModel == null) {
            LOG.error("No image model configured - defaultModel {} imageModel {}", defaultModel, imageModel);
            throw new IllegalArgumentException("Cannot use image as input, no image model configured.");
        }
        ChatCompletionRequest externalRequest = new ChatCompletionRequest();
        externalRequest.setModel(hasImage ? imageModel : defaultModel);
        externalRequest.setMessages(messages);
        externalRequest.setTemperature(temperature);
       Integer maxTokens = request.getMaxTokens();
        if (maxTokens != null && maxTokens > 0) {
            if (maximumTokensPerResponse != null && maximumTokensPerResponse > 0 && maxTokens > maximumTokensPerResponse) {
                LOG.debug("Reducing maxTokens from {} to {} because of configured maximumTokensPerResponse", maxTokens, maximumTokensPerResponse);
                maxTokens = maximumTokensPerResponse;
            }
            externalRequest.setMaxTokens(maxTokens);
        }
        externalRequest.setStream(Boolean.TRUE);
        String jsonRequest = gson.toJson(externalRequest);
        checkTokenCount(jsonRequest);
        return jsonRequest;
    }

    protected void checkEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("No API key configured for the GPT chat completion service. Please configure the service.");
        }
    }

    @Override
    public boolean isEnabled() {
        return !disabled;
    }

    @Override
    public boolean isEnabled(GPTConfiguration gptConfig) {
        return isEnabled() && (
                apiKey != null && !apiKey.trim().isEmpty() ||
                        gptConfig != null && gptConfig.getApiKey() != null && !gptConfig.getApiKey().trim().isEmpty()
        );
    }

    @Override
    public boolean isVisionEnabled() {
        return imageModel != null && !imageModel.trim().isEmpty();
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
    public String shorten(@Nullable String text, int maxTokens) {
        if (text == null) {
            return "";
        }
        List<Integer> markerTokens = enc.encodeOrdinary(TRUNCATE_MARKER);
        if (maxTokens <= markerTokens.size() + 6) {
            // this is absurd, probably usage error.
            LOG.warn("Cannot shorten text to {} tokens, too short. Returning original text.", maxTokens);
            return text;
        }

        List<Integer> encoded = enc.encodeOrdinary(text);
        if (encoded.size() <= maxTokens) {
            return text;
        }
        int borderTokens = (maxTokens - markerTokens.size()) / 2;
        List<Integer> result = encoded.subList(0, borderTokens);
        result.addAll(markerTokens);
        result.addAll(encoded.subList(encoded.size() - maxTokens + result.size(), encoded.size()));
        return enc.decode(result);
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
        return enc.countTokensOrdinary(text);
    }

    protected void checkTokenCount(String jsonRequest) {
        if (maximumTokensPerRequest != null && jsonRequest != null) {
            if (jsonRequest.length() < maximumTokensPerRequest) {
                return;
            }
            int tokens = countTokens(jsonRequest); // not exact but close enough for this purpose
            if (tokens > maximumTokensPerRequest) {
                throw new GPTException("Aborting request because configured maximumTokensPerRequest is exceeded: request has about " + tokens);
            }
        }
    }

    @Override
    @Nonnull
    public String htmlToMarkdown(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        return new HtmlToMarkdownConverter().convert(html).trim();
    }

    @ObjectClassDefinition(name = "Composum AI OpenAI Configuration",
            description = "Provides rather low level access to the GPT chat completion - use the other services for more specific services.")
    public @interface GPTChatCompletionServiceConfig {

        @AttributeDefinition(name = "Disable", description = "Disable the GPT Chat Completion Service", required = false)
        boolean disabled() default false; // we want it to work by just deploying it. Admittedly this is a bit doubtful.

        @AttributeDefinition(name = "URL of the chat completion service",
                description = "Optional, if not OpenAI's default " + CHAT_COMPLETION_URL, required = false)
        String chatCompletionUrl();

        @AttributeDefinition(name = "OpenAI API key", description = "OpenAI API key from https://platform.openai.com/. If not given, we check the key file, the environment Variable OPENAI_API_KEY, and the system property openai.api.key .", required = false)
        String openAiApiKey();

        @AttributeDefinition(name = "OpenAI Organization ID", description = "Optionally, OpenAI Organization ID from https://platform.openai.com/account/organization .", required = false)
        String openAiOrganizationId();

        // alternatively, a key file
        @AttributeDefinition(name = "OpenAI API key file", required = false,
                description = "Key File containing the API key, as an alternative to Open AKI Key configuration and the variants described there.")
        String openAiApiKeyFile();

        @AttributeDefinition(name = "Default model", required = false,
                description = "Default model to use for the chat completion. The default if not set is " + DEFAULT_MODEL + ". Please consider the varying prices https://openai.com/pricing .")
        String defaultModel() default DEFAULT_MODEL;

        @AttributeDefinition(name = "Vision model", required = false,
                description = "Optional, a model that is used if an image is given as input, e.g. gpt-4-vision-preview. If not given, image recognition is rejected.",
                defaultValue = DEFAULT_IMAGE_MODEL)
        String imageModel() default DEFAULT_IMAGE_MODEL;

        @AttributeDefinition(name = "Temperature", required = false,
                description = "Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0", defaultValue = "")
        String temperature();

        @AttributeDefinition(name = "Maximum Tokens per Request", description = "If > 0 limit to the maximum number of tokens per request. " +
                "That's about a twice the word count. Caution: Compare with the pricing - on GPT-4 models a thousand tokens might cost $0.01 or more.",
                defaultValue = "50000", required = false)
        int maximumTokensPerRequest();

        @AttributeDefinition(name = "Maximum output tokens per request", description = "Maximum number of tokens to return in the response. Must not exceed the capabilities of the model - as of 10/03/24 this is 4096 for most OpenAI models - which is the default, so no need to set that.", required = false)
        int maximumTokensPerResponse() default 4096;

        @AttributeDefinition(name = "Connection timeout in seconds", description = "Default " + DEFAULTVALUE_CONNECTIONTIMEOUT, required = false)
        int connectionTimeout() default DEFAULTVALUE_CONNECTIONTIMEOUT;

        @AttributeDefinition(name = "Request timeout in seconds", description = "Default " + DEFAULTVALUE_REQUESTTIMEOUT, required = false)
        int requestTimeout() default DEFAULTVALUE_REQUESTTIMEOUT;

        @AttributeDefinition(name = "Maximum requests per minute", required = false,
                description = "Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default " + DEFAULTVALUE_REQUESTS_PER_MINUTE)
        int requestsPerMinute();

        @AttributeDefinition(name = "Maximum requests per hour", required = false,
                description = "Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default " + DEFAULTVALUE_REQUESTS_PER_HOUR)
        int requestsPerHour();

        @AttributeDefinition(name = "Maximum requests per day", required = false,
                description = "Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default " + DEFAULTVALUE_REQUESTS_PER_DAY)
        int requestsPerDay();
    }

    /**
     * Thrown when we get a 429 rate limiting response.
     */
    protected static class RetryableException extends RuntimeException {
        public RetryableException(String errorMessage) {
            super(errorMessage);
        }
    }

    protected class StreamDecodingResponseConsumer extends AbstractCharResponseConsumer<Void> {

        protected final GPTCompletionCallback callback;
        protected final CompletableFuture<Void> result;
        protected final StringBuilder resultBuilder = new StringBuilder();
        protected final long id;

        /**
         * If set, we collect the data for the error message, of false we process it as stream.
         */
        protected Integer errorStatusCode;

        /**
         * The result of the webservice call is written to callback; result is set when either it completed or aborted.
         */
        public StreamDecodingResponseConsumer(GPTCompletionCallback callback, CompletableFuture<Void> result, long id) {
            this.callback = callback;
            this.result = result;
            this.id = id;
        }

        @Override
        protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
            if (response.getCode() != 200) {
                errorStatusCode = response.getCode();
                LOG.warn("Response {} from GPT is not 200, but {}", id, response.getCode());
            } else {
                LOG.debug("Response {} from GPT is 200", id);
            }
        }

        @Override
        protected void data(CharBuffer src, boolean endOfStream) throws IOException {
            LOG.trace("Response {} from GPT data part received {}", id, src);
            resultBuilder.append(src);
            if (errorStatusCode != null) {
                LOG.trace("Response {} from GPT error part received {}", id, src);
                return;
            }
            try {
                // while the resultBuilder contains a "\n" feed the line to handleStreamingEvent
                while (true) {
                    int pos = resultBuilder.indexOf("\n");
                    if (pos < 0) {
                        break;
                    }
                    String line = resultBuilder.substring(0, pos);
                    resultBuilder.delete(0, pos + 1);
                    handleStreamingEvent(callback, id, line);
                }
                if (endOfStream && resultBuilder.length() > 0) {
                    handleStreamingEvent(callback, id, resultBuilder.toString());
                }
            } catch (RuntimeException e) {
                LOG.error("Response {} from GPT data part received {} and failed", id, src, e);
                errorStatusCode = 700;
            }
        }

        @Override
        protected Void buildResult() throws IOException {
            LOG.trace("Response {} buildResult", id);
            // always called on request end.
            if (errorStatusCode != null) {
                if (errorStatusCode == 429) {
                    LOG.warn("Response {} from GPT is 429, retrying", id);
                    RetryableException retryableException = new RetryableException(resultBuilder.toString());
                    result.completeExceptionally(retryableException);
                    throw retryableException;
                }
                GPTException gptException = buildException(errorStatusCode, resultBuilder.toString());
                callback.onError(gptException);
                result.completeExceptionally(gptException);
                throw gptException;
            }
            result.complete(null);
            return null;
        }

        @Override
        public void failed(Exception cause) {
            LOG.info("Response {} from GPT failed: {}", id, cause.toString());
            result.completeExceptionally(cause);
            if (!(cause instanceof RetryableException)) {
                callback.onError(cause);
            }
        }

        @Override
        protected int capacityIncrement() {
            return 10000;
        }

        @Override
        public void releaseResources() {
            // nothing to do
        }

    }

    protected static GPTException buildException(Integer errorStatusCode, String result) {
        // this is annoyingly heuristic and seems to break once in a while.
        if (Integer.valueOf(400).equals(errorStatusCode) && result != null
                && result.contains("invalid_request_error") &&
                (result.contains("context_length_exceeded") || result.contains("max_tokens") ||
                        result.contains("model supports at most") ||
                        result.contains("maximum context length"))) {
            return new GPTException.GPTContextLengthExceededException(result);
        }
        return new GPTException("Error response from GPT (status " + errorStatusCode
                + ") : " + result);
    }

    /**
     * Makes doubly sure that result is somehow set after the call.
     */
    protected static class EnsureResultFutureCallback implements FutureCallback<Void> {

        @Nonnull
        protected final CompletableFuture<Void> result;

        public EnsureResultFutureCallback(@Nonnull CompletableFuture<Void> result) {
            this.result = result;
        }

        @Override
        public void completed(Void result) {
            if (!this.result.isDone()) {
                this.result.complete(result);
            }
        }

        @Override
        public void failed(Exception ex) {
            if (!this.result.isDone()) {
                this.result.completeExceptionally(ex);
            }
        }

        @Override
        public void cancelled() {
            if (!this.result.isDone()) {
                this.result.completeExceptionally(new CancellationException());
            }
        }
    }
}

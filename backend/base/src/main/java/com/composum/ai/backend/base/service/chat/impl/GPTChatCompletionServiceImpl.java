package com.composum.ai.backend.base.service.chat.impl;

import static java.util.Collections.singletonList;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.classic.methods.HttpPost;
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
import org.apache.hc.core5.http.HttpStatus;
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

import com.composum.ai.backend.base.service.chat.RateLimiter;
import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatMessagesTemplate;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTFinishReason;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.composum.ai.backend.base.service.chat.GPTTool;
import com.composum.ai.backend.base.service.chat.GPTToolCall;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionChoice;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionFunctionDetails;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionMessage;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionMessagePart;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionRequest;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionResponse;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatCompletionToolCall;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.ChatTool;
import com.composum.ai.backend.base.service.chat.impl.chatmodel.OpenAIEmbeddings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

/**
 * Implements the actual access to the ChatGPT chat API.
 *
 * @see "https://platform.openai.com/docs/api-reference/chat/create"
 * @see "https://platform.openai.com/docs/guides/chat"
 */
@Component(service = {GPTChatCompletionService.class, GPTInternalOpenAIHelper.class})
@Designate(ocd = GPTChatCompletionServiceImpl.GPTChatCompletionServiceConfig.class)
public class GPTChatCompletionServiceImpl extends GPTInternalOpenAIHelper.GPTInternalOpenAIHelperInst
        implements GPTChatCompletionService, GPTInternalOpenAIHelper {

    protected static final Logger LOG = LoggerFactory.getLogger(GPTChatCompletionServiceImpl.class);

    protected static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    protected static final Pattern PATTERN_TRY_AGAIN = Pattern.compile("Please try again in (\\d+)s.");

    public static final String DEFAULT_MODEL = "gpt-4o-mini";
    public static final String DEFAULT_IMAGE_MODEL = "gpt-4o";
    public static final String DEFAULT_EMBEDDINGS_MODEL = "text-embedding-3-small";
    public static final String DEFAULT_HIGH_INTELLIGENCE_MODEL = "gpt-4o";

    protected static final int DEFAULTVALUE_CONNECTIONTIMEOUT = 30;
    protected static final int DEFAULTVALUE_REQUESTTIMEOUT = 300;

    protected static final int DEFAULTVALUE_REQUESTS_PER_MINUTE = 100;
    protected static final int DEFAULTVALUE_REQUESTS_PER_HOUR = 1000;
    protected static final int DEFAULTVALUE_REQUESTS_PER_DAY = 3000;

    public static final String TRUNCATE_MARKER = " ... (truncated) ... ";
    /**
     * The maximum number of retries.
     */
    public static final int MAXTRIES = 5;

    protected String defaultModel;
    protected String highIntelligenceModel;
    protected String imageModel;

    protected CloseableHttpAsyncClient httpAsyncClient;

    protected static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

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
     * Tokenizer used for GPT-4 variants.
     */
    protected Encoding enc = registry.getEncoding(EncodingType.O200K_BASE);

    protected BundleContext bundleContext;

    protected final Map<String, GPTChatMessagesTemplate> templates = new HashMap<>();
    protected int requestTimeout;
    protected int connectionTimeout;

    protected boolean disabled;

    protected ScheduledExecutorService scheduledExecutorService;

    protected Integer maximumTokensPerRequest;
    protected Integer maximumTokensPerResponse;

    /**
     * Rate limiter for embeddings. These are a quite inexpensive service (0.13$ per million tokens), so
     * we just introduce a limit that should protect against malfunctions for now.
     */
    protected volatile RateLimiter embeddingsLimiter = new RateLimiter(
            new RateLimiter(null, 10000, 1, TimeUnit.DAYS),
            1000, 1, TimeUnit.MINUTES);

    protected String embeddingsUrl;
    protected String embeddingsModel;

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
        this.highIntelligenceModel = config != null && config.highIntelligenceModel() != null && !config.highIntelligenceModel().trim().isEmpty() ? config.highIntelligenceModel().trim() : DEFAULT_HIGH_INTELLIGENCE_MODEL;
        this.imageModel = config != null && config.imageModel() != null && !config.imageModel().trim().isEmpty() ? config.imageModel().trim() : null;
        this.requestTimeout = config != null && config.requestTimeout() > 0 ? config.requestTimeout() : DEFAULTVALUE_REQUESTTIMEOUT;
        this.connectionTimeout = config != null && config.connectionTimeout() > 0 ? config.connectionTimeout() : DEFAULTVALUE_CONNECTIONTIMEOUT;
        this.maximumTokensPerRequest = config != null && config.maximumTokensPerRequest() > 0 ? config.maximumTokensPerRequest() : null;
        this.maximumTokensPerResponse = config != null && config.maximumTokensPerResponse() > 0 ? config.maximumTokensPerResponse() : null;
        this.disabled = config != null && config.disabled();
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
        this.embeddingsUrl = config != null && config.embeddingsUrl() != null && !config.embeddingsUrl().trim().isEmpty() ? config.embeddingsUrl().trim() : OPENAI_EMBEDDINGS_URL;
        this.embeddingsModel = config != null && config.embeddingsModel() != null && !config.embeddingsModel().trim().isEmpty() ? config.embeddingsModel().trim() : DEFAULT_EMBEDDINGS_MODEL;
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
        this.defaultModel = null;
        this.imageModel = null;
        this.limiter = null;
        this.gptLimiter = null;
        this.bundleContext = null;
        this.templates.clear();
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
            scheduledExecutorService = null;
        }
    }

    @Override
    public String getSingleChatCompletion(@Nonnull GPTChatRequest request) throws GPTException {
        checkEnabled();
        waitForLimit();
        long id = requestCounter.incrementAndGet(); // to easily correlate log messages
        try {
            String jsonRequest = createJsonRequest(request);
            if (request.getConfiguration() != null && Boolean.TRUE.equals(request.getConfiguration().getDebug())) {
                LOG.debug("Not sending request {} to GPT - debugging mode: {}", id, jsonRequest);
                return jsonRequest;
            }
            LOG.debug("Sending request {} to GPT: {}", id, jsonRequest);

            SimpleHttpRequest httpRequest = makeRequest(jsonRequest, request.getConfiguration(), chatCompletionUrl);
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
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new GPTException("Execution Error while calling GPT", e);
        } catch (TimeoutException e) {
            LOG.error("" + e, e);
            throw new GPTException("Timeout while calling GPT", e);
        }
    }

    // XXX
    protected SimpleHttpRequest makeRequest(String jsonRequest, GPTConfiguration gptConfiguration, String url) {
        String actualApiKey = gptConfiguration != null && gptConfiguration.getApiKey() != null && !gptConfiguration.getApiKey().trim().isEmpty() ? gptConfiguration.getApiKey() : this.apiKey;
        String actualOrganizationId = gptConfiguration != null && gptConfiguration.getOrganizationId() != null && !gptConfiguration.getOrganizationId().trim().isEmpty() ? gptConfiguration.getOrganizationId() : this.organizationId;
        SimpleHttpRequest request = new SimpleHttpRequest("POST", url);
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

            if (request.getConfiguration() != null && Boolean.TRUE.equals(request.getConfiguration().getDebug())) {
                LOG.debug("Request not sent - debugging requested.");
                callback.onNext(jsonRequest);
                callback.onFinish(GPTFinishReason.STOP);
                return;
            }

            SimpleHttpRequest httpRequest = makeRequest(jsonRequest, request.getConfiguration(), chatCompletionUrl);
            performCallAsync(new CompletableFuture<>(), id, httpRequest, callback, 0, 2000);
            LOG.debug("Response {} from GPT is there and should be streaming", id);
        } catch (IOException e) {
            LOG.error("Error while call {} to GPT", id, e);
            throw new GPTException("Error while calling GPT", e);
        }
    }

    @Override
    public void streamingChatCompletionWithToolCalls(@Nonnull GPTChatRequest request, @Nonnull GPTCompletionCallback callback)
            throws GPTException {
        if (request.getConfiguration() == null || request.getConfiguration().getTools() == null || request.getConfiguration().getTools().isEmpty()) {
            streamingChatCompletion(request, callback);
            return;
        }
        GPTCompletionCallback callbackWrapper = new GPTCompletionCallback.GPTCompletionCallbackWrapper(callback) {
            List<GPTToolCall> collectedToolcalls = null;

            @Override
            public void toolDelta(List<GPTToolCall> toolCalls) {
                collectedToolcalls = GPTToolCall.mergeDelta(collectedToolcalls, toolCalls);
            }

            @Override
            public void onFinish(GPTFinishReason finishReason) {
                if (GPTFinishReason.TOOL_CALLS == finishReason) {
                    LOG.info("Executing tool calls");
                    LOG.debug("Tool calls: {}", collectedToolcalls);
                    GPTChatRequest requestWithToolCalls = request.copy();
                    GPTChatMessage assistantRequestsToolcallsMessage =
                            new GPTChatMessage(GPTMessageRole.ASSISTANT, null, null, null, collectedToolcalls);
                    requestWithToolCalls.addMessage(assistantRequestsToolcallsMessage);
                    for (GPTToolCall toolCall : collectedToolcalls) {
                        Optional<GPTTool> toolOption = request.getConfiguration().getTools().stream()
                                .filter(tool -> tool.getName().equals(toolCall.getFunction().getName()))
                                .findAny();
                        if (!toolOption.isPresent()) { // should be impossible
                            LOG.error("Bug: Tool {} not found in configuration", toolCall.getFunction().getName());
                            GPTException error = new GPTException("Bug: Tool " + toolCall.getFunction().getName() + " not found in configuration");
                            this.onError(error);
                            throw error;
                        }
                        GPTTool tool = toolOption.get();
                        String toolresult = tool.execute(toolCall.getFunction().getArguments(), getToolExecutionContext());
                        if (null == toolresult) {
                            toolresult = "";
                        }
                        LOG.debug("Tool {} with arguments {} returned {}", toolCall.getFunction().getName(),
                                toolCall.getFunction().getArguments(),
                                toolresult.substring(0, Math.min(100, toolresult.length())) + "...");
                        GPTChatMessage toolResponseMessage = new GPTChatMessage(GPTMessageRole.TOOL, toolresult, null, toolCall.getId(), null);
                        requestWithToolCalls.addMessage(toolResponseMessage);
                    }
                    streamingChatCompletionWithToolCalls(requestWithToolCalls, callback);
                } else {
                    super.onFinish(finishReason);
                }
            }
        };
        streamingChatCompletion(request, callbackWrapper);
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
            System.out.println(line);
            try {
                if (" [DONE]".equals(line)) {
                    LOG.debug("Response {} from GPT received DONE", id);
                    callback.close();
                    return;
                }
                ChatCompletionResponse chunk = gson.fromJson(line, ChatCompletionResponse.class);
                if (chunk == null || chunk.getChoices() == null || chunk.getChoices().isEmpty()) {
                    LOG.error("No chunks - id {} Cannot deserialize {}", id, line);
                    GPTException gptException = new GPTException("No chunks - cannot deserialize " + line);
                    callback.onError(gptException);
                    throw gptException;
                }
                ChatCompletionChoice choice = chunk.getChoices().get(0);
                String content = choice.getDelta().getContent();
                if (content != null && !content.isEmpty()) {
                    LOG.trace("Response {} from GPT: {}", id, content);
                    callback.onNext(content);
                }
                if (choice.getDelta().getToolCalls() != null) {
                    callback.toolDelta(ChatCompletionToolCall.toGptToolCallList(choice.getDelta().getToolCalls()));
                }
                if (choice.getFinishReason() != null) {
                    LOG.trace("Response {} from GPT finished with reason {}", id, choice.getFinishReason());
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
        // We also treat NullPointerException as retryable as this seems to happen randomly in a weird
        // at com.nr.agent.instrumentation.httpclient50.InstrumentationUtils.createInboundParams(InstrumentationUtils.java:80)
        // - probably doesn't hurt.
        RetryableException retryable = null;
        if (e instanceof RetryableException || e instanceof NullPointerException) {
            retryable = (RetryableException) e;
        } else if (e instanceof CompletionException) {
            CompletionException completionException = (CompletionException) e;
            if (completionException.getCause() instanceof RetryableException
                    || completionException.getCause() instanceof NullPointerException) {
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
        boolean highIntelligenceRequired = request.getConfiguration() != null && request.getConfiguration().highIntelligenceNeededIsSet();
        externalRequest.setModel(highIntelligenceRequired ? highIntelligenceModel : hasImage ? imageModel : defaultModel);
        externalRequest.setMessages(messages);
        externalRequest.setTemperature(request.getConfiguration() != null && request.getConfiguration().getTemperature() != null ?
                request.getConfiguration().getTemperature() : null);
        externalRequest.setSeed(request.getConfiguration() != null ? request.getConfiguration().getSeed() : null);
        if (request.getConfiguration() != null && request.getConfiguration().getAnswerType() == GPTConfiguration.AnswerType.JSON) {
            externalRequest.setResponseFormat(ChatCompletionRequest.JSON);
        }
        Integer maxTokens = request.getMaxTokens();
        if (maxTokens != null && maxTokens > 0) {
            if (maximumTokensPerResponse != null && maximumTokensPerResponse > 0 && maxTokens > maximumTokensPerResponse) {
                LOG.debug("Reducing maxTokens from {} to {} because of configured maximumTokensPerResponse", maxTokens, maximumTokensPerResponse);
                maxTokens = maximumTokensPerResponse;
            }
            externalRequest.setMaxTokens(maxTokens);
        }
        externalRequest.setStream(Boolean.TRUE);
        externalRequest.setTools(convertTools(request.getConfiguration()));
        String jsonRequest = gson.toJson(externalRequest);
        checkTokenCount(jsonRequest);
        return jsonRequest;
    }

    private List<ChatTool> convertTools(GPTConfiguration configuration) {
        if (configuration == null || configuration.getTools() == null || configuration.getTools().isEmpty()) {
            return null;
        }
        List<ChatTool> result = new ArrayList<>();
        for (GPTTool tool : configuration.getTools()) {
            ChatTool toolDescr = new ChatTool();
            ChatCompletionFunctionDetails details = new ChatCompletionFunctionDetails();
            details.setName(tool.getName());
            details.setStrict(true);
            Map declaration = gson.fromJson(tool.getToolDeclaration(), Map.class);
            Map function = (Map) declaration.get("function");
            details.setParameters(function.get("parameters"));
            details.setDescription((String) function.get("description"));
            toolDescr.setFunction(details);
            result.add(toolDescr);
        }
        return result;
    }

    protected void checkEnabled() {
        if (!isEnabled()) {
            throw new IllegalStateException("Not enabled or no API key configured for the GPT chat completion service. Please configure the service.");
        }
    }

    @Override
    public boolean isEnabled() {
        return !disabled;
    }

    @Override
    public boolean isEnabled(GPTConfiguration gptConfig) {
        return isEnabled(); // XXX
    }

    protected void checkEnabled(GPTConfiguration gptConfig) {
        checkEnabled();
        if (!isEnabled(gptConfig)) {
            throw new IllegalStateException("No API key configured for the GPT chat completion service. Please configure the service.");
        }
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
        IntArrayList markerTokens = enc.encodeOrdinary(TRUNCATE_MARKER);
        if (maxTokens <= markerTokens.size() + 6) {
            // this is absurd, probably usage error.
            LOG.warn("Cannot shorten text to {} tokens, too short. Returning original text.", maxTokens);
            return text;
        }

        IntArrayList encoded = enc.encodeOrdinary(text);
        if (encoded.size() <= maxTokens) {
            return text;
        }
        int borderTokens = (maxTokens - markerTokens.size()) / 2;
        IntArrayList result = new IntArrayList(maxTokens);
        for (int i = 0; i < borderTokens; i++) {
            result.add(encoded.get(i));
        }
        for (int i = 0; i < markerTokens.size(); i++) {
            result.add(markerTokens.get(i));
        }
        for (int i = encoded.size() - maxTokens + result.size(); i < encoded.size(); i++) {
            result.add(encoded.get(i));
        }
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

    @Override
    @Nonnull
    public List<float[]> getEmbeddings(List<String> texts, GPTConfiguration configuration) throws GPTException {
        if (texts == null || texts.isEmpty()) {
            return Collections.emptyList();
        }

        if (texts.contains(null) || texts.contains("")) {
            texts = texts.stream() // the API does not like empty strings
                    .map(text -> text == null || text.isEmpty() ? " " : text)
                    .collect(Collectors.toList());
        }

        checkEnabled(configuration);
        embeddingsLimiter.waitForLimit();
        long id = requestCounter.incrementAndGet(); // to easily correlate log messages
        List<float[]> result = getEmbeddingsImplDivideAndConquer(texts, configuration, id);
        return result;
    }

    protected List<float[]> getEmbeddingsImplDivideAndConquer(List<String> texts, GPTConfiguration configuration, long id) {
        try {
            return getEmbeddingsImpl(texts, configuration, id);
        } catch (GPTException.GPTContextLengthExceededException e) { // try divide and conquer
            if (texts.size() == 1) { // split text in half and ignore the rest. No chance but to loose information give up
                LOG.info("Context length exceeded for single text while call {} to GPT, loosing half of the text", id);
                String text = texts.get(0);
                if (text == null || text.length() < 100) {
                    throw new GPTException("Bug: unexpected context length exceeded exception", e);
                }
                return getEmbeddingsImplDivideAndConquer(singletonList(text.substring(0, text.length() / 2)), configuration, id);
            }
            List<String> firsthalf = texts.subList(0, texts.size() / 2);
            List<String> rest = texts.subList(texts.size() / 2, texts.size());
            // TODO: ideally that'd be in parallel
            List<float[]> firsthalfEmbeddings = getEmbeddingsImplDivideAndConquer(firsthalf, configuration, id);
            List<float[]> restEmbeddings = getEmbeddingsImplDivideAndConquer(rest, configuration, id);
            List<float[]> result = new ArrayList<>(firsthalfEmbeddings.size() + restEmbeddings.size());
            result.addAll(firsthalfEmbeddings);
            result.addAll(restEmbeddings);
            return result;
        }

    }

    protected List<float[]> getEmbeddingsImpl(List<String> texts, GPTConfiguration configuration, long id) {
        OpenAIEmbeddings.EmbeddingRequest request = new OpenAIEmbeddings.EmbeddingRequest();
        request.setInput(texts);
        request.setModel(embeddingsModel);
        request.setEncodingFormat("float");
        String jsonRequest = gson.toJson(request);
        LOG.trace("Sending embeddings request {} to GPT: {}", id, jsonRequest);
        SimpleHttpRequest httpRequest = makeRequest(jsonRequest, configuration, embeddingsUrl);
        Future<SimpleHttpResponse> call = httpAsyncClient.execute(httpRequest, null);
        String bodyText = null;
        try {
            SimpleHttpResponse response = call.get();
            bodyText = response.getBodyText();
            if (response.getCode() != HttpStatus.SC_OK) {
                LOG.info("Error while call {} to GPT: {} {}", id, response, bodyText);
                LOG.trace("Request was {}", jsonRequest);
                throw GPTException.buildException(response.getCode(), bodyText);
            }
            LOG.trace("Response {} from GPT: {}", id, bodyText);
            OpenAIEmbeddings.EmbeddingResponse entity = gson.fromJson(bodyText, OpenAIEmbeddings.EmbeddingResponse.class);
            if (entity.getData() == null) {
                LOG.error("No data in embeddings response {}", bodyText);
                throw new GPTException("No data in embeddings response");
            }
            float[][] result = new float[entity.getData().size()][];
            for (OpenAIEmbeddings.EmbeddingObject embeddingObject : entity.getData()) {
                result[embeddingObject.getIndex()] = embeddingObject.getEmbedding();
            }
            Arrays.stream(result).forEach(Objects::requireNonNull);
            return Arrays.asList(result);
        } catch (JsonSyntaxException e) {
            LOG.error("Cannot parse embeddings response because of {}", bodyText, e);
            throw new GPTException("Cannot parse embeddings response", e);
        } catch (InterruptedException e) {
            throw new GPTException("Interrupted while calling GPT", e);
        } catch (ExecutionException e) {
            throw new GPTException("Error while calling GPT", e.getCause());
        }
    }

    @Override
    public String getEmbeddingsModel() {
        return embeddingsModel;
    }

    @ObjectClassDefinition(name = "Composum AI OpenAI Configuration",
            description = "Provides rather low level access to the GPT chat completion - use the other services for more specific services.")
    public @interface GPTChatCompletionServiceConfig {

        @AttributeDefinition(name = "Disable", description = "Disable the GPT Chat Completion Service", required = false)
        boolean disabled() default false; // we want it to work by just deploying it. Admittedly this is a bit doubtful.

        // alternatively, a key file
        @AttributeDefinition(name = "OpenAI API key file", required = false,
                description = "Key File containing the API key, as an alternative to Open AKI Key configuration and the variants described there.")
        String openAiApiKeyFile();

        @AttributeDefinition(name = "Default model", required = false,
                description = "Default model to use for the chat completion. The default if not set is " + DEFAULT_MODEL + ". Please consider the varying prices https://openai.com/pricing .")
        String defaultModel() default DEFAULT_MODEL;

        @AttributeDefinition(name = "High intelligence model", required = false,
                description = "The model that is used for requests that need more reasoning performance. The default if not set is " + DEFAULT_HIGH_INTELLIGENCE_MODEL + ". Please consider the varying prices https://openai.com/pricing .")
        String highIntelligenceModel() default DEFAULT_HIGH_INTELLIGENCE_MODEL;

        @AttributeDefinition(name = "Vision model", required = false,
                description = "Optional, a model that is used if an image is given as input, e.g. gpt-4o. If not given, image recognition is rejected.",
                defaultValue = DEFAULT_IMAGE_MODEL)
        String imageModel() default DEFAULT_IMAGE_MODEL;

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

        @AttributeDefinition(name = "Embeddings model", required = false,
                description = "Optional model to use for the embeddings. The default is " + DEFAULT_EMBEDDINGS_MODEL + ".")
        String embeddingsModel() default DEFAULT_EMBEDDINGS_MODEL;
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
                GPTException gptException = GPTException.buildException(errorStatusCode, resultBuilder.toString());
                callback.onError(gptException);
                result.completeExceptionally(gptException);
                throw gptException;
            }
            result.complete(null);
            return null;
        }

        @Override
        public void failed(Exception cause) {
            LOG.info("Response {} from GPT failed: {}", id, cause.toString(), cause);
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

    @Override
    public GPTInternalOpenAIHelperInst getInstance() {
        return this;
    }

    @Override
    void initOpenAIRequest(@Nonnull HttpPost request, @Nullable GPTConfiguration gptConfiguration) {
        String actualApiKey = gptConfiguration != null && gptConfiguration.getApiKey() != null && !gptConfiguration.getApiKey().trim().isEmpty() ? gptConfiguration.getApiKey() : this.apiKey;
        String actualOrganizationId = gptConfiguration != null && gptConfiguration.getOrganizationId() != null && !gptConfiguration.getOrganizationId().trim().isEmpty() ? gptConfiguration.getOrganizationId() : this.organizationId;
        request.addHeader("Authorization", "Bearer " + actualApiKey);
        if (actualOrganizationId != null && !actualOrganizationId.trim().isEmpty()) {
            request.addHeader("OpenAI-Organization", actualOrganizationId);
        }
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

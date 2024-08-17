package com.composum.ai.backend.base.service.chat.impl;


import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.OPENAI_API_KEY;
import static com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.OPENAI_API_KEY_SYSPROP;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.io.CloseMode;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.impl.RateLimiter;
import com.composum.ai.backend.base.service.GPTException;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTDictationService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Component(service = GPTDictationService.class)
@Designate(ocd = GPTDictationServiceImpl.GPTDictationServiceConfig.class)
public class GPTDictationServiceImpl implements GPTDictationService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTDictationServiceImpl.class);

    public static final String URL_OPENAI_TRANSCRIPTIONS = "https://api.openai.com/v1/audio/transcriptions";

    protected static final int DEFAULTVALUE_REQUESTS_PER_MINUTE = 30;
    protected static final int DEFAULTVALUE_REQUESTS_PER_HOUR = 100;
    protected static final int DEFAULTVALUE_REQUESTS_PER_DAY = 300;
    protected static final String DEFAULT_MODEL = "whisper-1";

    private final Gson gson = new GsonBuilder().create();

    protected CloseableHttpClient httpClient;
    protected RateLimiter limiter;
    protected boolean enabled;
    protected String apiKey;
    protected String organizationId;
    protected String model;
    protected long maxRequestSize = 1000000;

    @Activate
    protected void activate(GPTDictationServiceConfig config) throws URISyntaxException {
        this.enabled = config != null && config.enabled();
        if (enabled) {
            httpClient = HttpClients.createSystem();
            // httpClient = HttpClients.custom().setProxy(HttpHost.create("localhost:8080")).build();

            // since it costs a bit of money and there are remote limits, we do limit it somewhat, especially for the case of errors.
            int limitPerDay = config.requestsPerDay() > 0 ? config.requestsPerDay() : DEFAULTVALUE_REQUESTS_PER_DAY;
            RateLimiter dayLimiter = new RateLimiter(null, limitPerDay, 1, TimeUnit.DAYS);
            int limitPerHour = config.requestsPerHour() > 0 ? config.requestsPerHour() : DEFAULTVALUE_REQUESTS_PER_HOUR;
            RateLimiter hourLimiter = new RateLimiter(dayLimiter, limitPerHour, 1, TimeUnit.HOURS);
            int limitPerMinute = config.requestsPerMinute() > 0 ? config.requestsPerMinute() : DEFAULTVALUE_REQUESTS_PER_MINUTE;
            this.limiter = new RateLimiter(hourLimiter, limitPerMinute, 1, TimeUnit.MINUTES);
            this.apiKey = config.apiKey();
            this.organizationId = config.organizationId();
            this.model = config.model();
            this.maxRequestSize = config.maxRequestSize() > 0 ? config.maxRequestSize() : 1000000;
        }
    }

    @Deactivate
    protected void deactivate() throws IOException {
        this.enabled = false;
        if (null != this.httpClient) {
            this.httpClient.close(CloseMode.IMMEDIATE);
            this.httpClient = null;
        }
    }

    @Modified
    protected void modified(GPTDictationServiceConfig config) throws IOException, URISyntaxException {
        if (null != this.httpClient) {
            this.httpClient.close(CloseMode.GRACEFUL);
            this.httpClient = null;
        }
        activate(config);
    }

    @Override
    public boolean isAvailable(@Nullable GPTConfiguration configuration) {
        return enabled && retrieveOpenAIKey(configuration) != null;
    }

    @Override
    public String transcribe(@Nonnull InputStream audioStream, @Nonnull String contentType, @Nullable String language,
                             @Nullable GPTConfiguration configuration, @Nullable String prompt) {
        if (!isAvailable(configuration)) {
            throw new IllegalStateException("GPT Dictation Service is not available.");
        }
        String currentApiKey = retrieveOpenAIKey(configuration);
        if (currentApiKey == null) {
            throw new IllegalStateException("OpenAI API key is not available.");
        }
        String currentOrganizationId = configuration != null && configuration.getOrganizationId() != null &&
                !configuration.getOrganizationId().isEmpty() ?
                configuration.getOrganizationId() :
                this.organizationId;

        limiter.waitForLimit();

        try {
            String url = URL_OPENAI_TRANSCRIPTIONS;
            HttpPost postRequest = new HttpPost(url);
            postRequest.addHeader(new BasicHeader("Authorization", "Bearer " + currentApiKey));
            if (currentOrganizationId != null && !currentOrganizationId.trim().isEmpty()) {
                postRequest.addHeader(new BasicHeader("OpenAI-Organization", currentOrganizationId.trim()));
            }
            postRequest.setEntity(createEntity(audioStream, contentType, prompt, language));

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                if (response.getCode() == HttpStatus.SC_OK) {
                    String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    return body;
                } else {
                    String body = "";
                    try {
                        body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        LOG.debug("Error reading error response body", e);
                    }
                    LOG.debug("Transcription error: " + response.getCode() + " " + response.getReasonPhrase() + " : " + body);
                    throw new GPTException("Transcription error: " + response.getCode() + " " + response.getReasonPhrase() + " : " + body);
                }
            }
        } catch (ParseException | IOException e) {
            throw new GPTException("Transcription error: " + e.getMessage(), e);
        }
    }

    private HttpEntity createEntity(InputStream audioStream, String contentType, String prompt, String language) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.STRICT);
        builder.addTextBody("model", "whisper-1", ContentType.TEXT_PLAIN);
        builder.addTextBody("response_format", "text", ContentType.TEXT_PLAIN);
        if (prompt != null && !prompt.trim().isEmpty()) {
            builder.addTextBody("prompt", prompt);
        }
        if (language != null && !language.trim().isEmpty()) {
            builder.addTextBody("language", language);
        }
        builder.addBinaryBody("file",
                new LimitedInputStream(audioStream, maxRequestSize),
                ContentType.create(contentType), "audio");

        HttpEntity multipart = builder.build();
        return multipart;
    }

    protected String retrieveOpenAIKey(@Nullable GPTConfiguration config) {
        String apiKey = null;
        if (config != null) {
            apiKey = config.getApiKey();
            if (apiKey != null && !apiKey.trim().isEmpty() && !apiKey.startsWith("$[secret")) {
                LOG.info("Using OpenAI API key from configuration.");
                return apiKey.trim();
            }
        }
        if (this.apiKey != null && !this.apiKey.trim().isEmpty()) {
            return this.apiKey.trim();
        }
        apiKey = System.getenv(OPENAI_API_KEY);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            LOG.info("Using OpenAI API key from environment variable.");
            return apiKey.trim();
        }
        apiKey = System.getProperty(OPENAI_API_KEY_SYSPROP);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            LOG.info("Using OpenAI API key from system property.");
            return apiKey.trim();
        }
        return null;
    }

    /**
     * Configures whether it's enabled (default false), the model and the request counts, and the maximum request size.
     */
    @ObjectClassDefinition(name = "GPT Dictation Service Configuration")
    public @interface GPTDictationServiceConfig {

        @AttributeDefinition(name = "Enabled", description = "Whether the service is enabled and properly configured.")
        boolean enabled() default false;

        @AttributeDefinition(name = "Model", description = "The model to use for dictation.", defaultValue = "")
        String model() default DEFAULT_MODEL;

        @AttributeDefinition(name = "Maximum requests per minute", required = false,
                description = "Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default " + DEFAULTVALUE_REQUESTS_PER_MINUTE, defaultValue = "")
        int requestsPerMinute() default DEFAULTVALUE_REQUESTS_PER_MINUTE;

        @AttributeDefinition(name = "Maximum requests per hour", required = false,
                description = "Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default " + DEFAULTVALUE_REQUESTS_PER_HOUR, defaultValue = "")
        int requestsPerHour() default DEFAULTVALUE_REQUESTS_PER_MINUTE;

        @AttributeDefinition(name = "Maximum requests per day", required = false,
                description = "Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default " + DEFAULTVALUE_REQUESTS_PER_DAY, defaultValue = "")
        int requestsPerDay() default DEFAULTVALUE_REQUESTS_PER_MINUTE;

        @AttributeDefinition(name = "Maximum request size", required = false,
                description = "Maximum request size in bytes, default 100000.", defaultValue = "")
        int maxRequestSize() default 1000000;

        @AttributeDefinition(name = "OpenAI API Key", required = false,
                description = "The API key for OpenAI, if not set here, it will be read from the environment variable " + OPENAI_API_KEY + " or the system property " + OPENAI_API_KEY_SYSPROP, defaultValue = "")
        String apiKey();

        @AttributeDefinition(name = "Organization ID", required = false,
                description = "The organization ID for OpenAI, optional.", defaultValue = "")
        String organizationId();
    }

    protected class LimitedInputStream extends FilterInputStream {

        private final long maxSize;
        private long bytesRead;

        protected LimitedInputStream(InputStream in, long maxSize) {
            super(in);
            this.maxSize = maxSize;
            this.bytesRead = 0;
        }

        @Override
        public int read() throws IOException {
            throw new UnsupportedOperationException("Not implemented yet.");
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (bytesRead >= maxSize) {
                return -1; // End of stream
            }
            int bytesToRead = (int) Math.min(len, maxSize - bytesRead);
            int result = super.read(b, off, bytesToRead);
            if (result > 0) {
                bytesRead += result;
            }
            return result;
        }
    }
}

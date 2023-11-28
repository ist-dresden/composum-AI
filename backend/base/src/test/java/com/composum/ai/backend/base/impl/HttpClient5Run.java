package com.composum.ai.backend.base.impl;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.hc.client5.http.async.methods.AbstractCharResponseConsumer;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.sling.commons.threads.ThreadPool;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proof of concept for calling OpenAI with Apache Http Client 5 asynchronously.
 */
public class HttpClient5Run {

    private static final Logger LOG = LoggerFactory.getLogger(HttpClient5Run.class);

    private ExecutorService executor = Executors.newFixedThreadPool(5);
    // mock threadPool so that it calls real methods on executor
    private ThreadPool threadPool = Mockito.mock(ThreadPool.class, Mockito.withSettings().defaultAnswer(invocation -> {
        Object[] args = invocation.getArguments();
        return invocation.getMethod().invoke(executor, args);
    }));

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        new HttpClient5Run().run();
    }

    private void run() throws InterruptedException, ExecutionException {
        try {
            // stateless, no cookies, execution on threadPool
            CloseableHttpAsyncClient client = HttpAsyncClients.custom().build();
            client.start();
            String requestBody = "{\n" +
                    "    \"model\": \"gpt-3.5-turbo\",\n" +
                    "    \"messages\": [\n" +
                    "      {\n" +
                    "        \"role\": \"user\",\n" +
                    "        \"content\": \"Hi!\"\n" +
                    "      }\n" +
                    "    ]\n" +
                    "  }";
            SimpleHttpRequest request = new SimpleHttpRequest("POST", "https://api.openai.com/v1/chat/completions");
            request.setBody(requestBody, ContentType.APPLICATION_JSON);
            request.addHeader("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"));
            SimpleRequestProducer requestProducer = SimpleRequestProducer.create(request);
            TestingResponseConsumer responseConsumer = new TestingResponseConsumer();
            Future<String> result = client.execute(requestProducer, responseConsumer, null);
            System.out.println("result: " + result.get());
        } finally {
            executor.shutdownNow();
        }

    }

    private class TestingResponseConsumer extends AbstractCharResponseConsumer<String> {

        StringBuilder result = new StringBuilder();

        @Override
        protected void start(HttpResponse response, ContentType contentType) throws HttpException, IOException {
            LOG.error("TestingResponseConsumer.start: {}", response);
        }

        @Override
        protected String buildResult() throws IOException {
            return result.toString();
        }

        @Override
        protected int capacityIncrement() {
            return 10000;
        }

        @Override
        protected void data(CharBuffer src, boolean endOfStream) throws IOException {
            LOG.error("TestingResponseConsumer.data: {}", src);
            result.append(src);
        }

        @Override
        public void releaseResources() {
            LOG.error("TestingResponseConsumer.releaseResources");
        }
    }
}

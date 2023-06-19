package com.composum.ai.backend.base.service.chat.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.theokanning.openai.OpenAiError;
import com.theokanning.openai.completion.chat.ChatCompletionChunk;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;

/**
 * Try streaming with the completion interface.
 */
public class StreamingRunner {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingRunner.class);

    public static void main(String[] args) throws Exception {
        try {
            new StreamingRunner().run();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    ObjectMapper mapper = new ObjectMapper();

    private void run() throws IOException, InterruptedException {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Path filePath = Paths.get(System.getProperty("user.home"), ".openaiapi");
        String apiKey = Files.readString(filePath).trim();
        List<ChatMessage> messages = new ArrayList<>();
        String content = "";
//        for (int i = 0; i < 10; i++) { // 10000 for error
//            content = content + " " + i;
//        }
//        content = content + " What is the first and what is the last number in this message?";
        content = "Create a haiku about the weather.";
        messages.add(new ChatMessage("user", content));
        ChatCompletionRequest externalRequest = ChatCompletionRequest.builder()
                .model(GPTChatCompletionServiceImpl.DEFAULT_MODEL)
                .messages(messages)
                .stream(true)
                .build();
        String jsonRequest = mapper.writeValueAsString(externalRequest);
        // System.out.println(jsonRequest);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(GPTChatCompletionServiceImpl.CHAT_COMPLETION_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
        if (response.statusCode() == 200) {
            response.body().forEach(this::handleLine);
        } else {
            String body = response.body().collect(Collectors.joining(""));
            OpenAiError error = mapper.readerFor(OpenAiError.class).readValue(body);
            System.out.println(error);
        }
        // print all headers of response
        response.headers().map().forEach((k, v) -> System.out.println(k + ": " + v));
        // result:  {"id":"chatcmpl-7PF2mSly0Vt200mHpNshcxGKCrXTM","object":"chat.completion.chunk","created":1686250384,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{"role":"assistant"},"index":0,"finish_reason":null}]}
        // {"id":"chatcmpl-7RnobyLIyEVNSrc3AKIAukowesabD","object":"chat.completion.chunk","created":1686860701,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{"content":"Rain"},"index":0,"finish_reason":null}]}
        // {"id":"chatcmpl-7RnobyLIyEVNSrc3AKIAukowesabD","object":"chat.completion.chunk","created":1686860701,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{"content":"drops"},"index":0,"finish_reason":null}]}
        // The first number in this message is 0 and the last number is 9. {"id":"chatcmpl-7PF2mSly0Vt200mHpNshcxGKCrXTM","object":"chat.completion.chunk","created":1686250384,"model":"gpt-3.5-turbo-0301","choices":[{"delta":{},"index":0,"finish_reason":"stop"}]}
        //DONE
    }

    private void handleLine(String s) {
        if (s.startsWith("data:")) {
            s = s.substring(5);
            System.out.println(s);
            if (" [DONE]".equals(s)) {
                System.out.println("DONE");
                return;
            }
            try {
                ChatCompletionChunk chunk = mapper.readerFor(ChatCompletionChunk.class).readValue(s);
                String content = chunk.getChoices().get(0).getMessage().getContent();
                if (content != null) {
                    System.out.print(content);
                    System.out.flush();
                } else {
                    System.out.println(s);
                    String finishReason = chunk.getChoices().get(0).getFinishReason();
                    if ("stop".equals(finishReason)) {
                        throw new IllegalStateException("STOP");
                    }
                }
            } catch (IOException e) {
                LOG.error("Cannot deserialize " + s, e);
                throw new IllegalStateException(e);
            }
        } else if (s != null && !s.isBlank()) {
            System.out.println(s);
        }
    }
}

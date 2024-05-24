package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChatCompletionModelTest {

    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    private String removeWhitespaceAndNewlines(String str) {
        return str.replaceAll("\\s+", "");
    }

    @Test
    public void testRequestSerializationDeserialization() {
        String originalRequestJson =
                "{\n" +
                        "  \"model\": \"gpt-3.5-turbo\",\n" +
                        "  \"messages\": [\n" +
                        "    {\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},\n" +
                        "    {\"role\": \"assistant\", \"content\": \"Hello!\"},\n" +
                        "    {\"role\": \"user\", \"content\": [\n" +
                        "      {\"type\": \"text\", \"text\": \"What?s in this image?\"},\n" +
                        "      {\"type\": \"image_url\", \"image_url\": {\"url\": \"https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg\", \"detail\":\"low\"}}\n" +
                        "    ]}\n" +
                        "  ],\n" +
                        "  \"max_tokens\": 300,\n" +
                        "  \"stream\": true\n" +
                        "}";
        ChatCompletionRequest request = gson.fromJson(originalRequestJson, ChatCompletionRequest.class);
        String serializedRequestJson = gson.toJson(request);
        assertEquals(removeWhitespaceAndNewlines(originalRequestJson), removeWhitespaceAndNewlines(serializedRequestJson));
    }

    @Test
    public void testResponseSerializationDeserialization() {
        String originalResponseJson =
                "{\n" +
                        "  \"id\": \"chatcmpl-123\",\n" +
                        "  \"object\": \"chat.completion\",\n" +
                        "  \"created\": 1677652288,\n" +
                        "  \"model\": \"gpt-3.5-turbo-0613\",\n" +
                        "  \"system_fingerprint\": \"fp_44709d6fcb\",\n" +
                        "  \"choices\": [\n" +
                        "    {\"index\": 0, \"message\": {\"role\": \"assistant\", \"content\": \"Hello there, how may I assist you today?\"}, \"finish_reason\": \"stop\"}\n" +
                        "  ],\n" +
                        "  \"usage\": {\"prompt_tokens\": 9, \"completion_tokens\": 12, \"total_tokens\": 21}\n" +
                        "}";
        ChatCompletionResponse response = gson.fromJson(originalResponseJson, ChatCompletionResponse.class);
        String serializedResponseJson = gson.toJson(response);
        assertEquals(removeWhitespaceAndNewlines(originalResponseJson), removeWhitespaceAndNewlines(serializedResponseJson));
    }

    @Test
    public void testChunkSerializationDeserialization() {
        String originalChunkJson = "{\n" +
                "  \"id\": \"chatcmpl-8YBAbKcTCwOzh6EklnSCJE2k44NOU\",\n" +
                "  \"object\": \"chat.completion.chunk\",\n" +
                "  \"created\": 1703156781,\n" +
                "  \"model\": \"gpt-3.5-turbo-0613\",\n" +
                "  \"system_fingerprint\": null,\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"delta\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"\"\n" +
                "      },\n" +
                "      \"logprobs\": null,\n" +
                "      \"finish_reason\": null\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String expectedChunkJson = "{\n" +
                "  \"id\": \"chatcmpl-8YBAbKcTCwOzh6EklnSCJE2k44NOU\",\n" +
                "  \"object\": \"chat.completion.chunk\",\n" +
                "  \"created\": 1703156781,\n" +
                "  \"model\": \"gpt-3.5-turbo-0613\",\n" +
                "  \"choices\": [\n" +
                "    {\n" +
                "      \"index\": 0,\n" +
                "      \"delta\": {\n" +
                "        \"role\": \"assistant\",\n" +
                "        \"content\": \"\"\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"; // without the null values
        ChatCompletionResponse response = gson.fromJson(originalChunkJson, ChatCompletionResponse.class);
        String serializedChunkJson = gson.toJson(response);
        assertEquals(removeWhitespaceAndNewlines(expectedChunkJson), removeWhitespaceAndNewlines(serializedChunkJson));
    }
}

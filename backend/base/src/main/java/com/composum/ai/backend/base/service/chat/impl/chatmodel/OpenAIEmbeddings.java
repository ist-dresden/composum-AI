package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request and response for OpenAI embeddings API.
 *
 * @see "https://platform.openai.com/docs/api-reference/embeddings"
 */
// generated with this prompt from the embeddings text and example requests / response, with some unused stuff removed:
// aigenpipeline -p embeddings.prompt ChatCompletionRequest.java -o Embeddings.java
// Create an interface Embeddings with inner classes EmbeddingRequest, EmbeddingResponse and EmbeddingObject according to the following documentation. Follow the given example of the ChatCompletionRequest with the structuring of the requests. For the input we need only the array of strings variant. The classes will be serialized / deserialized with GSON.
public interface OpenAIEmbeddings {

    class EmbeddingRequest {

        @JsonProperty("input")
        private List<String> input;

        @JsonProperty("model")
        private String model;

        @JsonProperty("encoding_format")
        private String encodingFormat;

        @JsonProperty("dimensions")
        private Integer dimensions;

        @JsonProperty("user")
        private String user;

        // Getters and setters
        public List<String> getInput() {
            return input;
        }

        public void setInput(List<String> input) {
            this.input = input;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getEncodingFormat() {
            return encodingFormat;
        }

        public void setEncodingFormat(String encodingFormat) {
            this.encodingFormat = encodingFormat;
        }

        public Integer getDimensions() {
            return dimensions;
        }

        public void setDimensions(Integer dimensions) {
            this.dimensions = dimensions;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }

    class EmbeddingResponse {

        @JsonProperty("data")
        private List<EmbeddingObject> data;

        // Getters and setters
        public List<EmbeddingObject> getData() {
            return data;
        }

        public void setData(List<EmbeddingObject> data) {
            this.data = data;
        }

    }

    class EmbeddingObject {

        @JsonProperty("index")
        private Integer index;

        @JsonProperty("embedding")
        private float[] embedding;

        // Getters and setters
        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public float[] getEmbedding() {
            return embedding;
        }

        public void setEmbedding(float[] embedding) {
            this.embedding = embedding;
        }
    }

}

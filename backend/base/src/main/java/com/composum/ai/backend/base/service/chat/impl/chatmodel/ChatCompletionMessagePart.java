package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Represents a part of a chat completion message, which may be a text or an image URL.
 * This allows messages to include multiple types of content.
 * <pre><code>
 *          {
 *           "type": "text",
 *           "text": "What’s in this image?"
 *         }
 *         or
 *         {
 *           "type": "image_url",
 *           "image_url": {
 *             "url": "https://www.example.net/somepicture.jpg"
 *           }
 *         }
 * </code></pre>
 */
public class ChatCompletionMessagePart {

    /**
     * The type of this message part, either 'text' or 'image_url'.
     */
    @JsonProperty("type")
    private Type type;
    /**
     * The text content of this message part, used when the type is 'text'.
     */
    @JsonProperty("text")
    private String text;
    /**
     * The image URL content of this message part, used when the type is 'image_url'.
     */
    @JsonProperty("image_url")
    private ChatCompletionMessageUrlPart imageUrl;

    public static ChatCompletionMessagePart text(String text) {
        ChatCompletionMessagePart part = new ChatCompletionMessagePart();
        part.setType(Type.TEXT);
        part.setText(text);
        return part;
    }

    // Getters and setters

    public static ChatCompletionMessagePart imageUrl(String imageUrl) {
        ChatCompletionMessagePart part = new ChatCompletionMessagePart();
        part.setType(Type.IMAGE_URL);
        ChatCompletionMessageUrlPart urlpart = new ChatCompletionMessageUrlPart();
        urlpart.setUrl(imageUrl);
        part.setImageUrl(urlpart);
        return part;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ChatCompletionMessageUrlPart getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(ChatCompletionMessageUrlPart image_url) {
        this.imageUrl = image_url;
    }

    public boolean isEmpty(Void ignoreJustPreventSerialization) {
        return (text == null || text.isEmpty()) &&
                (imageUrl == null || imageUrl.getUrl() == null || imageUrl.getUrl().isEmpty());
    }

    public enum Type {
        @JsonProperty("text")
        TEXT,
        @JsonProperty("image_url")
        IMAGE_URL
    }

    public enum ImageDetail {
        @JsonProperty("low")
        LOW,
        @JsonProperty("high")
        HIGH
    }

    /**
     * Encodes URL part: { "url": "https://example.com/somepicture.jpg" }
     */
    public static class ChatCompletionMessageUrlPart {

        @JsonProperty("url")
        private String url;

        @JsonProperty("detail")
        private ImageDetail detail = ImageDetail.LOW;

        // Getters and setters

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public ImageDetail getDetail() {
            return detail;
        }

        public void setDetail(ImageDetail detail) {
            this.detail = detail;
        }

    }

    public static class ChatCompletionMessagePartListDeserializer extends JsonDeserializer<List<ChatCompletionMessagePart>> {

        @Override
        public List<ChatCompletionMessagePart> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            List<ChatCompletionMessagePart> content = new ArrayList<>();
            JsonNode json = p.getCodec().readTree(p);

            if (json.isArray()) {
                for (JsonNode element : json) {
                    try {
                        content.add(p.getCodec().treeToValue(element, ChatCompletionMessagePart.class));
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            } else if (json.isTextual()) {
                ChatCompletionMessagePart part = new ChatCompletionMessagePart();
                part.setText(json.asText());
                part.setType(Type.TEXT);
                content.add(part);
            }

            return content;
        }

    }

    /**
     * To save space: if there is only one element in the list that also is a text message, we serialize it as a
     * string, otherwise as object list. An empty or null list is omitted entirely, like the other null fields.
     */
    public static class ChatCompletionMessagePartListSerializer extends JsonSerializer<List<ChatCompletionMessagePart>> {

        @Override
        public boolean isEmpty(SerializerProvider provider, List<ChatCompletionMessagePart> value) {
            return value == null || value.isEmpty();
        }

        @Override
        public void serialize(List<ChatCompletionMessagePart> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value.size() == 1 && value.get(0).getType() == Type.TEXT) {
                gen.writeString(value.get(0).getText());
            } else {
                serializers.defaultSerializeValue(value, gen);
            }
        }

    }

}

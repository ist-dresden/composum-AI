package com.composum.ai.backend.base.service.chat.impl.chatmodel;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

/**
 * A text part or image part of a chat completion message.
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

    public enum Type {
        @SerializedName("text")
        TEXT,
        @SerializedName("image_url")
        IMAGE_URL
    }

    @SerializedName("type")
    private Type type;

    @SerializedName("text")
    private String text;

    @SerializedName("image_url")
    private ChatCompletionMessageUrlPart imageUrl;

    // Getters and setters

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

    public static ChatCompletionMessagePart text(String text) {
        ChatCompletionMessagePart part = new ChatCompletionMessagePart();
        part.setType(Type.TEXT);
        part.setText(text);
        return part;
    }

    public static ChatCompletionMessagePart imageUrl(String imageUrl) {
        ChatCompletionMessagePart part = new ChatCompletionMessagePart();
        part.setType(Type.IMAGE_URL);
        ChatCompletionMessageUrlPart urlpart = new ChatCompletionMessageUrlPart();
        urlpart.setUrl(imageUrl);
        part.setImageUrl(urlpart);
        return part;
    }

    /**
     * Encodes URL part: { "url": "https://example.com/somepicture.jpg" }
     */
    public static class ChatCompletionMessageUrlPart {

        @SerializedName("url")
        private String url;

        // Getters and setters

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

    }

    public static class ChatCompletionMessagePartListDeSerializer implements JsonDeserializer<List<ChatCompletionMessagePart>>,
            JsonSerializer<List<ChatCompletionMessagePart>> {

        @Override
        public List<ChatCompletionMessagePart> deserialize(JsonElement json, java.lang.reflect.Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<ChatCompletionMessagePart> content = new ArrayList<>();

            if (json.isJsonArray()) {
                for (JsonElement element : json.getAsJsonArray()) {
                    try {
                        content.add(context.deserialize(element, ChatCompletionMessagePart.class));
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        throw e;
                    }

                }
            } else if (json.isJsonPrimitive()) {
                ChatCompletionMessagePart part = new ChatCompletionMessagePart();
                part.setText(json.getAsString());
                part.setType(Type.TEXT);
                content.add(part);
            }

            return content;
        }

        /**
         * To save space: if there is only one element in src that also is a text message, we serialize it as a string,
         * otherwise as object list.
         */
        @Override
        public JsonElement serialize(List<ChatCompletionMessagePart> src, java.lang.reflect.Type typeOfSrc, JsonSerializationContext context) {
            if (src == null || src.isEmpty()) {
                return null;
            }
            if (src.size() == 1 && src.get(0).getType() == Type.TEXT) {
                return context.serialize(src.get(0).getText(), String.class);
            }
            return context.serialize(src);
        }

    }

}

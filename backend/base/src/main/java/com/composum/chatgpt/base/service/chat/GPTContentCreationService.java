package com.composum.chatgpt.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service to generate content (keywords / descriptions from a text, and so forth.)
 */
public interface GPTContentCreationService {

    /**
     * Generates a list of keywords from the given text.
     *
     * @param text The text to generate keywords from.
     * @return A list of generated keywords, possibly empty.
     */
    @Nonnull
    List<String> generateKeywords(@Nullable String text);

    /**
     * Generates a description from the given text.
     *
     * @param text The text to generate a description from.
     * @param maxwords approximate maximum number of words in the description, if > 0
     * @return A generated description, possibly empty.
     */
    @Nonnull
    String generateDescription(@Nullable String text, int maxwords);

}

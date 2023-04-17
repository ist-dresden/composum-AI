package com.composum.chatgpt.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service to generate keywords from a text.
 */
public interface GPTKeywordService {

    /**
     * Generates a list of keywords from the given text.
     *
     * @param text The text to generate keywords from.
     * @return A list of generated keywords, possibly empty.
     */
    @Nonnull
    List<String> generateKeywords(@Nullable String text);

}

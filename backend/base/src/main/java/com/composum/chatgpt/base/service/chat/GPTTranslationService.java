package com.composum.chatgpt.base.service.chat;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.composum.chatgpt.base.service.GPTException;

/**
 * Building on {@link GPTChatCompletionService} this implements translation.
 */
public interface GPTTranslationService {

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    @Nonnull
    String singleTranslation(@Nullable String text, @Nullable String sourceLanguage, @Nullable String targetLanguage) throws GPTException;

}

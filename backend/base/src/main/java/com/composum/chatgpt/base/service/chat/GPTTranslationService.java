package com.composum.chatgpt.base.service.chat;

import javax.annotation.Nonnull;

/**
 * Building on {@link GPTChatCompletionService} this implements translation.
 */
public interface GPTTranslationService {

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    String singleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage);

}

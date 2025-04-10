package com.composum.ai.backend.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.composum.ai.backend.base.service.GPTException;

/**
 * Building on {@link GPTChatCompletionService} this implements translation.
 */
public interface GPTTranslationService {

    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     * @param text the text to translate
     * @param sourceLanguage the language to translate from - human readable name, or null for autodetect
     * @param targetLanguage the language to translate to - human readable name
     * @param configuration the configuration to use
     */
    @Nullable
    String singleTranslation(@Nullable String text, @Nullable String sourceLanguage, @Nullable String targetLanguage, @Nullable GPTConfiguration configuration) throws GPTException;


    /**
     * Translate the text from the target to destination language, either Java locale name or language name.
     */
    void streamingSingleTranslation(@Nonnull String text, @Nonnull String sourceLanguage, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration, @Nonnull GPTCompletionCallback callback) throws GPTException;


    /**
     * Translates the texts into the target language. The texts should belong together, like e.g. the texts of a page,
     * since the translations might influence each other. (We try to translate them in one request.)
     *
     * @param texts          the texts to translate
     * @param targetLanguage the language to translate to - human readable name
     * @return the translated texts
     */
    @Nonnull
    default List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration) throws GPTException {
        return fragmentedTranslation(texts, targetLanguage, configuration, null);
    }

    /**
     * Translates the texts into the target language. The texts should belong together, like e.g. the texts of a page,
     * since the translations might influence each other. (We try to translate them in one request.)
     *
     * @param texts             the texts to translate
     * @param targetLanguage    the language to translate to - human readable name
     * @param configuration     the configuration to use
     * @param translationChecks additional checks to verify the translation
     * @return the translated texts
     */
    @Nonnull
    List<String> fragmentedTranslation(@Nonnull List<String> texts, @Nonnull String targetLanguage, @Nullable GPTConfiguration configuration,
                                       @Nullable List<GPTResponseCheck> translationChecks) throws GPTException;

}

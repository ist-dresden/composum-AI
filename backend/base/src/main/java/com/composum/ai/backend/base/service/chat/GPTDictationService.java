package com.composum.ai.backend.base.service.chat;

import java.io.InputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Services related to dictation.
 */
public interface GPTDictationService {

    /**
     * Whether the service is enabled and properly configured.
     */
    boolean isAvailable(@Nullable GPTConfiguration configuration);

    /**
     * Transcribes the input audio to text.
     * @param audioStream the audio stream to transcribe, will be closed
     * @param contentType the content type of the audio, e.g. "audio/mpeg" for mp3, "audio/wav" for wav
     * @param language the language code to use, e.g. "en" for English, or null for automatic detection
     * @param configuration the configuration to use, or null for the default configuration
     * @param prompt an optional prompt to give the AI some context, e.g. previous sentences
     * @exception IllegalStateException if the service is not available / configured
     */
    String transcribe(@Nonnull InputStream audioStream, @Nonnull String contentType, @Nullable String language,
                      @Nullable GPTConfiguration configuration, @Nullable String prompt)
            throws IllegalStateException;

}

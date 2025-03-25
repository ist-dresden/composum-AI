package com.composum.ai.backend.base.service.chat.impl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.message.BasicHttpRequest;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;

/**
 * Helpers for implementation in this package.
 */
public interface GPTInternalOpenAIHelper {

    /**
     * Returns a helper for implementation in this package. We do this indirection to make it only available
     * for this package, since otherwise everything is public in an interface.
     */
    GPTInternalOpenAIHelperInst getInstance();

    /**
     * Checks whether ChatGPT is generally and whether gptConfig enables executing GPT calls.
     * (That is currently whether there is an api key either globally or in the gptConfig).
     */
    boolean isEnabled(GPTConfiguration gptConfig);

    abstract class GPTInternalOpenAIHelperInst {

        /**
         * Sets the request headers appropriate for authorization.
         */
        abstract void initRequest(@Nonnull BasicHttpRequest request, @Nullable GPTConfiguration configuration);

    }
}

package com.composum.ai.backend.slingbase.model;

import javax.annotation.Nonnull;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;

/**
 * For Sling tools: the request and response of the streaming as executin context for tool calls.
 */
public class SlingGPTExecutionContext implements GPTCompletionCallback.GPTToolExecutionContext {

    @Nonnull
    private final SlingHttpServletRequest request;

    @Nonnull
    private final SlingHttpServletResponse response;

    public SlingGPTExecutionContext(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    @Nonnull
    public SlingHttpServletRequest getRequest() {
        return request;
    }

    @Nonnull
    public SlingHttpServletResponse getResponse() {
        return response;
    }

}

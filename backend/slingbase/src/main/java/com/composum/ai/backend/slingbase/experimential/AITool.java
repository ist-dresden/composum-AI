package com.composum.ai.backend.slingbase.experimential;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import com.composum.ai.backend.base.service.chat.GPTCompletionCallback;
import com.composum.ai.backend.base.service.chat.GPTCompletionCallback.GPTToolExecutionContext;
import com.composum.ai.backend.base.service.chat.GPTTool;

/**
 * An action the AI can perform - likely from the sidebar chat.
 *
 * @see "https://platform.openai.com/docs/guides/function-calling"
 */
public interface AITool {

    /**
     * Human readable name.
     */
    @Nonnull
    String getName(@Nullable Locale locale);

    /**
     * Human readable description.
     */
    @Nonnull
    String getDescription(@Nullable Locale locale);

    /**
     * Name for the purpose of calling - must match {@link #getToolDeclaration()}.
     */
    @Nonnull
    String getToolName();

    /**
     * The description to use for the OpenAI tool call. Will be inserted into the OpenAI tools array. E.g.:
     * <code><pre>
     *       {
     *         "type": "function",
     *         "function": {
     *           "name": "get_delivery_date",
     *           "description": "Get the delivery date for a customer's order. Call this whenever you need to know the delivery date, for example when a customer asks 'Where is my package'",
     *           "parameters": {
     *             "type": "object",
     *             "properties": {
     *               "order_id": {
     *                 "type": "string",
     *                 "description": "The customer's order ID."
     *               }
     *             },
     *             "required": ["order_id"],
     *             "additionalProperties": false
     *           }
     *         },
     *         "strict": true
     *       }
     * </pre></code>
     *
     * @see "https://platform.openai.com/docs/api-reference/chat/create"
     */
    @Nonnull
    String getToolDeclaration();

    /**
     * Whether the tool is enabled for the given resource.
     */
    boolean isAllowedFor(@Nullable Resource resource);

    /**
     * Executes the tool call and returns the result to present to the AI.
     * Must only be called if {@link #isAllowedFor(Resource)} returned true.
     */
    @Nonnull
    String execute(@Nullable String arguments, @Nonnull Resource resource,
                   @Nullable GPTCompletionCallback.GPTToolExecutionContext context);

    /**
     * The form useable by {@link com.composum.ai.backend.base.service.chat.GPTChatCompletionService}.
     */
    @Nullable
    default GPTTool makeGPTTool(@Nonnull Resource resource,
                       @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        if (!isAllowedFor(resource)) {
            return null;
        }
        return new GPTTool() {
            @Override
            public @Nonnull String getName() {
                return AITool.this.getToolName();
            }

            @Override
            public @Nonnull String getToolDeclaration() {
                return AITool.this.getToolDeclaration();
            }

            @Override
            public @Nonnull String execute(@Nullable String arguments, @Nullable GPTToolExecutionContext context) {
                return AITool.this.execute(arguments, resource, context);
            }
        };
    }

}

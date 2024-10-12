package com.composum.ai.backend.slingbase.experimential;

import java.util.Locale;

import org.apache.sling.api.resource.Resource;

/**
 * An action the AI can perform - likely from the sidebar chat.
 *
 * @see "https://platform.openai.com/docs/guides/function-calling"
 */
public interface AITool {

    /**
     * Human readable name.
     */
    String getName(Locale locale);

    /**
     * Human readable description.
     */
    String getDescription(Locale locale);

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
    String getToolDeclaration();

    /**
     * Whether the tool is enabled for the given resource.
     */
    public boolean isAllowedFor(Resource resource);

    /**
     * Executes the tool call and returns the result to present to the AI.
     * Must only be called if {@link #isAllowedFor(Resource)} returned true.
     */
    public String execute(String arguments, Resource resource);

}

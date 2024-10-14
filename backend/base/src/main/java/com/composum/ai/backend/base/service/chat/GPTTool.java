package com.composum.ai.backend.base.service.chat;

/**
 * An action the AI can perform - likely from the sidebar chat.
 *
 * @see "https://platform.openai.com/docs/guides/function-calling"
 */
public interface GPTTool {

    /**
     * The name of the tool - must be exactly the name given in {@link #getToolDeclaration()}.
     */
    String getName();

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
     * Executes the tool call and returns the result to present to the AI.
     */
    public String execute(String arguments);

}

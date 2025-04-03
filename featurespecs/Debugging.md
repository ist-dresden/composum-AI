# Some debugging hints

## retrieving the request to the AI

If one of these markers is put into the prompt, certain actions are done:

- DEBUGADDINSTRUCTIONS09483748 during translation throws an GPTUserNotificationException containing the additional
  instructions. This is used in the translation test UIs /apps/composum-ai/components/autotranslate/list.html
  or /apps/composum-ai/components/autotranslate-experiments/list.html
  (see [automatic translation(https://ai.composum.com/aem-variant/automaticTranslation.html)])
- DEBUGPRINTREQUEST34856385 throws an exception containing the request that would be sent to the AI.
- DEBUGOUTPUTREQUEST4398592 similarily outputs the JSON request that would be sent to the AI to the normal output 
  (does work in dialogs, not in the translation)

To try variations of the AI requests you can import and test this printed JSON with
https://chatgpttools.stoerr.net/chatgpttools/multiplemessagechat.html

The requests and responses are also logged in the server logfiles, but that might be invisible depending on the log
level.

## Various hints

To find the code chain that is active in an action involving the AI you could perform the action locally and create
a breakpoint in the "heart" GPTChatCompletionServiceImpl , e.g. in
com.composum.ai.backend.base.service.chat.impl.GPTChatCompletionServiceImpl.makeRequest
and study the stacktrace.


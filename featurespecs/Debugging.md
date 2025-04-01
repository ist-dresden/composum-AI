# Some debugging hints

## retrieving the request to the AI

If one of these markers is put into the prompt, certain actions are done:

- DEBUGADDINSTRUCTIONS09483748 during translation throws an GPTUserNotificationException containing the additional
  instructions. This is used in the translation test UIs /apps/composum-ai/components/autotranslate/list.html
  or /apps/composum-ai/components/autotranslate-experiments/list.html
  (see [automatic translation(https://ai.composum.com/aem-variant/automaticTranslation.html)])
- DEBUGPRINTREQUEST34856385 throws an exception containing the request that would be sent to the AI.
- DEBUGOUTPUTREQUEST4398592 similarily outputs the JSON request that would be sent to the AI to the normal output.

To try variations of the AI requests you can import and test this printed JSON with
https://chatgpttools.stoerr.net/chatgpttools/multiplemessagechat.html

The requests and responses are also logged in the server logfiles, but that might be invisible depending on the log
level.

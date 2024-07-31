# Analysis of about the ChatGPT API wrt. our needs

At least initially we will restrict ourselves to the [ChatGPT chat API](https://platform.openai.com/docs/guides/chat) 
with gpt-4o-mini , since that model is likely appropriate and sufficient for our purposes, and rather 
[competitively priced](https://openai.com/pricing) at
$0.15 / 1M tokens (= about 600000-700000 words), as of 8/2024.

The CHATGPT API is also used for local models with e.g. [LM Studio](https://lmstudio.ai/) or 
[ollama](https://github.com/ollama/ollama) - since the URL is configurable, they could be used, too, though a >= 
GPT-3.5 performance model is needed. 

## Libraries

There is a list of [ChatGPT community libraries](https://platform.openai.com/docs/libraries/community-libraries)
that has a [openai-java](https://github.com/TheoKanning/openai-java) library. It has several parts - an API module
containing model classes with appropriate JSON annotations for the calls, which is handy, and a service
implementation that has quite a lot of dependencies we would need to deploy. The service module would likely
introduce more complications than it's worth, so we just take the API module - Jackson and HttpClient 4 are
deployed on Composum systems, anyway.

## Links
- [ChatGPT chat API](https://platform.openai.com/docs/guides/chat)
- https://platform.openai.com/docs/api-reference/chat/create
- https://platform.openai.com/playground?mode=chat

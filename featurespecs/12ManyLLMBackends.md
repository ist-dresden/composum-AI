# Many LLM Backends

## Basic Idea

While the OpenAI models are very good for many tasks there is now quite a competition in the LLM market and LLM models
are increasingly becoming a commodity. If you are
translating into many languages, where the performance of the OpenAI models might not be as good, it pays to have a
choice. Also, for privacy reasons having a choice might be helpful for some customers, since some LLM service
providers might run in the users own country or relevant jurisdictions like the EU. So we will integrate alternative
backends into the Composum AI.

## Requirements

- It should be possible to switch the model by OSGI configuration or by Sling Context Aware Configuration, in order
  to make things easily changeable on AEMaaCS. That applies to both the API keys for models, as well as which model
  to choose for a particular JCR subtree.
- There should be model configurations (each model is identified by a key) and configurations that choose which
  model to use for a task.
- It should be possible to configure independently which model to use for the content creation assistant + side
  panel AI and for the translation.

Supported backends are:

- OpenAI API and compatible servers
- Anthropic Claude
- Google Gemini
- Microsoft Azure OpenAI service
- Microsoft Azure [AI Foundry](https://learn.microsoft.com/en-us/azure/ai-studio/what-is-ai-studio)
- ??? AWS Bedrock
- ??? IONOS

Since OpenAI API is used by many local model servers like [ollama](https://ollama.com/), we make two HTTP headers
configurable (both header and value), which should support many providers.

## Information about the individual backends

### OpenAI API

The [Chat Completion API](https://platform.openai.com/docs/api-reference/chat) is used. That's compatible to the
current `ChatCompletion*` classes in `com.composum.ai.backend.base.service.chat.impl.chatmodel`.
Header: `Authorization: Bearer $OPENAI_API_KEY`

### Google Gemini API

The [Google Gemini API](https://ai.google.dev/gemini-api/docs) can be accessed
[compatibly to OpenAI API](https://ai.google.dev/gemini-api/docs/openai), though the primary interface
[is different](https://ai.google.dev/api/generate-content).

- Header: `Authorization: Bearer $GEMINI_API_KEY`
- Endpoint: https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
- [Model list](https://ai.google.dev/gemini-api/docs/models/gemini):
  `curl https://generativelanguage.googleapis.com/v1beta/openai/models -H "Authorization: Bearer $GEMINI_API_KEY"`
    - e.g. gemini-2.0-flash-lite , gemini-1.5-pro , gemini-2.0-flash , gemini-2.0-pro-exp ,
      gemini-2.0-flash-thinking-exp
- Java library: https://ai.google.dev/gemini-api/docs/sdks#java with quite a lot
  [dependencies](https://central.sonatype.com/artifact/com.google.genai/google-genai/dependencies) , though.

## [Anthropic Claude](https://www.anthropic.com/)

While having a quite different [native API](https://docs.anthropic.com/en/api/messages), Anthropic Claude offers a
[OpenAI compatibility endpoint](https://docs.anthropic.com/en/api/openai-sdk) which covers practically everything we
use as well. So we can avoid creating an alternative backend there.

`curl https://api.anthropic.com/v1/models -H "x-api-key: $ANTHROPIC_API_KEY" -H "anthropic-version: 2023-06-01" | jq`

## Azure AI Foundry

https://learn.microsoft.com/en-us/rest/api/aifoundry/modelinference/ seems to be OpenAI compatible, too.
https://learn.microsoft.com/en-us/azure/ai-foundry/concepts/deployments-overview : Model as a service with 
serverless API deployment are billed per token.

## [Amazon Bedrock](https://aws.amazon.com/de/bedrock/)

The [new Converse API](https://docs.aws.amazon.com/bedrock/latest/APIReference/API_runtime_Converse.html) is not 
OpenAI compatible. Since everything else is OpenAI compatible and that would generate quite some effort to 
support completely different interfaces, we will currently not support that.

## Configuration

We create an OSGI configuration factory for backend configurations, as well as a SlingCAConfig that configures the
backends. Per backend the following things are necessary:

- URL
- Headers for Authorization - in the case of OpenAI: api key and organization
- ? high intelligence model, low intelligence model
- ? available models

Possibilities for model configuration on the translation:

- model id for model selection purposes
- specify model name
- choose configuration + model name
- additional configurations: temperature, seed, maximum tokens, timeout, maximum request counts
-

Hierarchy: configuration AI provider that has Models

? extend com.composum.ai.backend.slingbase.model.OpenAIConfig

## Architecture

The implementation is mostly done in the `backend/base` module.
For backwards compatibility we leave the `ChatCompletion*` classes in
`com.composum.ai.backend.base.service.chat.impl.chatmodel` for OpenAI compatible backend as they are.

### Examples for implementation details

- com.composum.ai.backend.slingbase.model.OpenAIConfig is both Sling CA Config as well as OSGI configuration
- OsgiAIConfigurationPluginFactoryImpl is an example for a factory
- 

## Implementation decisions

- We leave the current OpenAI configuration as it is as a default, with identifiers `default` and `default-hi` for the
  default model and high intelligfence model.
- Embeddings will only be dealt using that default configuration, as they are rarely used yet.

## TODO

- configuration print page? model test page?
- where should I configure the model for content creation / sidebar AI?

GPTPromptLibrary example OSGI configuration and Sling CA configuration

### Questions

- configuration secrets only OSGI?

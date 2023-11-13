# Configuration of the Composum AI within Composum Pages

The configuration is designed so that the Composum API is easy to try out without any configuration besides an
OpenAI API key used to access the
[ChatGPT chat completions API](https://platform.openai.com/docs/guides/text-generation/chat-completions-api)
for the whole site. This makes the AI available in all places it supports. But it is possible to restrict the usage
of the AI e.g. to page trees, certain page templates or components, as well as to certain users and groups.

Generally, the configuration has several levels of fallbacks. If present,
[Sling Context Aware Configuration]
(https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html)
overrides all other configuration methods. That falls back to an OSGI configuration, for which there are sometimes
other fallbacks.

## OpenAI API Key

<div style="float: right; margin-left: 20px;">
   <a href="../image/ai/configuration/OpenAiConfiguration.png">
    <img src="../image/ai/configuration/OpenAiConfiguration.png" alt="Sidebar AI" width="400" />
  </a>
</div>

Using the Composum AI needs an [OpenAI API key](https://platform.openai.com/api-keys) that can either be configured for
the whole server in the OSGI configuration "Composum AI GPT Chat Completion Service" (or "Composum AI OpenAI
Configuration") or flexibly per site or
content-tree with Sling Context Aware Configuration in the "Composum AI OpenAI Configuration".

For the OpenAI key there is a fallback hierarchy:

- Sling Context Aware Configuration with the configuration class
  `com.composum.ai.backend.slingbase.model.OpenAIConfig`
- OSGI configuration at "Composum AI GPT Chat Completion Service"
- Environment variable OPENAI_API_KEY
- System property openai.api.key

For the OSGI configuration there are the following configurations:

| Configuration Key | Description                                                                                                                | Default Value |
|-------------------|----------------------------------------------------------------------------------------------------------------------------|---------------|
| disabled          | Disable the GPT Chat Completion Service                                                                                    | false         |
| openAiApiKey      | OpenAI API Key from https://platform.openai.com/. If not given, checks key file, environment Variable OPENAI_API_KEY, etc. |               |
| openAiApiKeyFile  | OpenAI API Key File containing the API key, as an alternative to Open AI Key configuration                                 |               |
| defaultModel      | Default model to use for the chat completion. Consider varying prices https://openai.com/pricing                           | gpt-3.5-turbo |
| temperature       | Optional temperature setting that determines variability vs. creativity as a floating point between 0.0 and 1.0            |               |
| connectionTimeout | Connection timeout in seconds                                                                                              | 20            |
| requestTimeout    | Request timeout in seconds                                                                                                 | 60            |

If Sling Context Aware Configuration contains an entry for `com.composum.ai.backend.slingbase.model.OpenAIConfig`,
then the OpenAI API Key is taken from the configuration `openAiApiKey` of that `Composum AI OpenAI Configuration` of
the edited page or experience fragment. If that is not present, the fallback hierarchy is used.

<div style="clear: both;"></div>

## AI Permission Configuration

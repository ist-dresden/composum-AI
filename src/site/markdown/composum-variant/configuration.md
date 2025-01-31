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

See also the [OSGI configuration reference](../autogenerated/osgiconfigurations.md) /
[Sling CA configuration reference](../autogenerated/slingcaconfigurations.md) for the Composum AI.

## LLM API Key and general configuration

<div style="float: right; margin-left: 20px;">
   <a href="../image/ai/configuration/OpenAiConfiguration.png">
    <img src="../image/ai/configuration/OpenAiConfiguration.png" alt="LLM configuration" width="400" />
  </a>
</div>

The Composum AI can be configured to use OpenAI services or other LLM that have a compatible interface, like local LLM.

### Configuration to use OpenAI services

Using the Composum AI needs an [OpenAI API key](https://platform.openai.com/api-keys) that can either be configured for
the whole server in the OSGI configuration "Composum AI OpenAI Configuration" or flexibly per site or
content-tree with Sling Context Aware Configuration in the "Composum AI OpenAI Configuration".

For the OpenAI key there is a fallback hierarchy:

- Sling Context Aware Configuration with the configuration class
  `com.composum.ai.backend.slingbase.model.OpenAIConfig`
- OSGI configuration at "Composum AI OpenAI Configuration"
- Environment variable OPENAI_API_KEY
- System property openai.api.key

For the OSGI configuration there are the following configurations:

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| disabled | Disable | Boolean | false | Disable the GPT Chat Completion Service |
| chatCompletionUrl | URL of the chat completion service | String |  | Optional, if not OpenAI's default https://api.openai.com/v1/chat/completions |
| openAiApiKey | OpenAI API key | String |  | OpenAI API key from https://platform.openai.com/. If not given, we check the key file, the environment Variable OPENAI_API_KEY, and the system property openai.api.key . |
| openAiOrganizationId | OpenAI Organization ID | String |  | Optionally, OpenAI Organization ID from https://platform.openai.com/account/organization . |
| openAiApiKeyFile | OpenAI API key file | String |  | Key File containing the API key, as an alternative to Open AKI Key configuration and the variants described there. |
| defaultModel | Default model | String | gpt-4o-mini | Default model to use for the chat completion. The default if not set is gpt-4o-mini. Please consider the varying prices https://openai.com/pricing . |
| highIntelligenceModel | High intelligence model | String | gpt-4o | The model that is used for requests that need more reasoning performance. The default if not set is gpt-4o. Please consider the varying prices https://openai.com/pricing . |
| imageModel | Vision model | String | gpt-4o | Optional, a model that is used if an image is given as input, e.g. gpt-4o. If not given, image recognition is rejected. |
| temperature | Temperature | String |  | Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0 |
| maximumTokensPerRequest | Maximum Tokens per Request | Integer | 50000 | If > 0 limit to the maximum number of tokens per request. That's about a twice the word count. Caution: Compare with the pricing - on GPT-4 models a thousand tokens might cost $0.01 or more. |
| maximumTokensPerResponse | Maximum output tokens per request | Integer | 4096 | Maximum number of tokens to return in the response. Must not exceed the capabilities of the model - as of 10/03/24 this is 4096 for most OpenAI models - which is the default, so no need to set that. |
| connectionTimeout | Connection timeout in seconds | Integer | 20 | Default 20 |
| requestTimeout | Request timeout in seconds | Integer | 120 | Default 120 |
| requestsPerMinute | Maximum requests per minute | Integer | 100 | Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default 100 |
| requestsPerHour | Maximum requests per hour | Integer | 1000 | Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default 1000 |
| requestsPerDay | Maximum requests per day | Integer | 3000 | Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default 3000 |
| embeddingsUrl | URL of the embeddings service | String |  | Optional, if not OpenAI's default https://api.openai.com/v1/embeddings |
| embeddingsModel | Embeddings model | String | text-embedding-3-small | Optional model to use for the embeddings. The default is text-embedding-3-small. |

If Sling Context Aware Configuration contains an entry for `com.composum.ai.backend.slingbase.model.OpenAIConfig`,
then the OpenAI API Key is taken from the configuration `openAiApiKey` of that `Composum AI OpenAI Configuration` of
the edited page or experience fragment. If that is not present, the fallback hierarchy is used. The context aware
configuration permits using different OpenAI API keys for different parts of the site. If only a single key is
desired, a global default can be set at e.g.
/conf/global/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig

```json
{
  "jcr:primaryType": "nt:unstructured",
  "openAiApiKey": "sk-******"
}
```

where sk-****** should be replaced by the actual OpenAI API key.

<div style="clear: both;"></div>

<a name="promptlibconf"></a>

### Using local LLM

There are 
[many ways to run open source LLM locally](https://medium.com/thedeephub/50-open-source-options-for-running-llms-locally-db1ec6f5a54f)
, e.g. [LM Studio](https://lmstudio.ai/). Often these offer interfaces like the OpenAI chat completion service, and 
do not need any API key. In that case you'll just need to configure the chatCompletionUrl to the appropriate value 
of the server.

### Using other LLM

Using other LLM might need some small modifications for authorization headers and possibly a different interface - if
you need that please [contact us](https://www.composum.com/home/contact.html).

## Composum AI Prompt Library Configuration

Composum AI provides a default prompt library for the Content Creation Assistant and the Side Panel Assistant. It is
quite likely that you will want to customize the prompts or add new prompts for your specific use case, or use prompts
in a different language than english. This can be
done with the "Composum AI Prompt Library Configuration". There is an OSGI configuration that can set system-wide
default paths, and a Sling Context Aware Configuration that can override these defaults for specific parts of the site.

| id                         | label                         | type   | default value | description                           |
|----------------------------|-------------------------------|--------|---------------|---------------------------------------|
| contentCreationPromptsPath | Content Creation Prompts Path | String |               | Path to the content creation prompts. |
| sidePanelPromptsPath       | Side Panel Prompts Path       | String |               | Path to the side panel prompts.       |

These paths can be paths to a JSON file like
[predefinedprompts.json](https://github.com/ist-dresden/composum-AI/blob/develop/composum/bundle/src/main/resources/create/predefinedprompts.json)
. To make it easy to maintain a prompt library, it is also possible to use content pages as prompt library. The
content page should have a simple structure and the prompts should be as components in exactly one paragraph system
containing text components with the prompts. The subtitle in the text component is taken as the title of the prompt, and
the text is taken as the text of the prompt.

If you want to adapt the default prompt libraries
`/libs/composum/pages/options/ai/dialogs/create/defaultprompts`
`/libs/composum/pages/options/ai/tools/sidebar/defaultprompts`
: these can be copied into the site or some other place and edited there.

<div style="clear: both;"></div>

## AI Permission Configuration

<div style="float: right; margin-left: 20px;">
   <a href="../image/ai/configuration/AIPermissionConfiguration.png">
    <img src="../image/ai/configuration/AIPermissionConfiguration.png" alt="Sidebar AI" width="400" />
  </a>
</div>

We also use a fallback hierarchy for the permission configuration:

- Sling Context Aware Configuration
- an OSGI configuration factory at "Composum AI Permission Configuration"
- a single OSGI configuration at "Composum AI Permission Configuration" that is the default configuration when
  nothing else is found, and normally allows all services for all users etc. if not changed.

The context aware configuration and the OSGI configuration factory both allow configuring lists of configurations to
allow configuring different permissions for different parts of the site. Permissions are thus additive. If any of the
fallback levels has an entry, the lower fallback levels are not considered at all. Thus, if the context aware
configuration is not used and the factory is not used either, then the default "all allowing" configuration is used,
which it is ignored otherwise. If using context aware configuration it's sensible to provide a default
configuration at e.g.
/conf/global/sling:configs/com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration/entry1
that forbids everything for sites where sling context aware configuration is not configured as a default, and then
add configurations for the parts where it is allowed.

For the sling context aware configuration, we have a configuration list at
`com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration` , which is also used in the OSGI configuration.
It has the following properties:

| Configuration Key    | Description                                                                                                                                                                    | Default Value                            |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|
| services             | List of services to which this configuration applies. Possible values are: "categorize", "create", "sidepanel", "translate" . For AEM only create and sidepanel are supported. | categorize, create, sidepanel, translate |
| allowedUsers         | Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration.                                                              | .*                                       |
| deniedUsers          | Regular expressions for denied users or user groups. Takes precedence over allowed users.                                                                                      |                                          |
| allowedPaths         | Regular expressions for allowed content paths. If not present, no paths are allowed.                                                                                           | /content/.*                              |
| deniedPaths          | Regular expressions for denied content paths. Takes precedence over allowed paths.                                                                                             | /content/dam/.*                          |
| allowedViews         | Regular expressions for allowed views - that is, for URLs like /editor.html/.* . If not present, no views are allowed. Use .* to allow all views.                              | .*                                       |
| deniedViews          | Regular expressions for denied views. Takes precedence over allowed views.                                                                                                     |                                          |
| allowedComponents    | Regular expressions for allowed resource types of components. If not present, no components are allowed.                                                                       | .*                                       |
| deniedComponents     | Regular expressions for denied resource types of components. Takes precedence over allowed components.                                                                         |                                          |
| allowedPageTemplates | Regular expressions for allowed page templates. If not present, all page templates are allowed.                                                                                | .*                                       |
| deniedPageTemplates  | Regular expressions for denied page templates. Takes precedence over allowed page templates.                                                                                   |                                          |

The general principle is that a configuration allows using one or more services (that is, assistant dialogs) for
everything that matches the allows* properties and does not match the denied* properties - thus the denied takes
precedence.

It is possible to create several list entries allowing a subset of the services (dialogs) for a subset of users,
sites, components and so forth. Permissions are additive.

Example: for allowing all services for all users etc. on the subtree /content/wknd you could have the following entry at
`/conf/wknd/sling:configs/com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration/entry1`
if `/content/wknd/jcr:content` has a `sling:configRef` pointing to `/conf/wknd`.

```json
{
  "jcr:primaryType": "nt:unstructured",
  "services": [
    "categorize",
    "create",
    "sidepanel",
    "translate"
  ],
  "allowedUsers": [
    ".*"
  ],
  "deniedUsers": [
    ".*"
  ],
  "allowedPaths": [
    "/content/.*"
  ],
  "deniedPaths": [
    "/content/dam/.*"
  ],
  "allowedViews": [
    ".*"
  ],
  "deniedViews": [
    ""
  ],
  "allowedComponents": [
    ".*"
  ],
  "deniedComponents": [
    ""
  ],
  "allowedPageTemplates": [
    ".*"
  ],
  "deniedPageTemplates": [
    ""
  ]
}
```

## Specification

For even more details see the
[specification](https://github.com/ist-dresden/composum-AI/blob/develop/featurespecs/6Configuration.md)

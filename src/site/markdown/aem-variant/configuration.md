# Configuration of Composum AI in AEM

Using the Composum AI needs an [OpenAI API key](https://platform.openai.com/api-keys) that can either be configured for
the whole server in the OSGI configuration "Composum AI GPT Chat Completion Service" or flexibly per site or
content-tree with Sling Context Aware Configuration in the "Composum AI OpenAI Configuration".

If you want Composum AI to be available to the editors whereever it's supported, you only need to configure the
OpenAI API key. But it is also possible to restrict Composum AI via OSGI or context aware configuration to certain
users / groups, components, paths, page templates and views with the permission configuration.

Generally we support
[Sling Context Aware Configuration](https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html)
and fall back to OSGI configuration. The context aware configuration can be edited e.g. with the
[wcm.io configuration editor](https://wcm.io/caconfig/editor/) .

## OpenAI API Key and general configuration

<div style="float: right; margin-left: 20px;">
   <a href="../image/ai/configuration/OpenAiConfiguration.png">
    <img src="../image/ai/configuration/OpenAiConfiguration.png" alt="Sidebar AI" width="400" />
  </a>
</div>

The Composum AI can be configured to use OpenAI services or other LLM that have a compatible interface, like local LLM.

### Configuration to use OpenAI services

Using the Composum AI needs an [OpenAI API key](https://platform.openai.com/api-keys) that can either be configured for
the whole server in the OSGI configuration "Composum AI GPT Chat Completion Service" (or "Composum AI OpenAI
Configuration") or flexibly per site or
content-tree with Sling Context Aware Configuration in the "Composum AI OpenAI Configuration".

For the OpenAI key there is a fallback hierarchy:

- Sling Context Aware Configuration with the configuration class
  `com.composum.ai.backend.slingbase.model.OpenAIConfig`
- OSGI configuration at "Composum AI GPT Chat Completion Service" / "Composum AI OpenAI Configuration"
- Environment variable OPENAI_API_KEY
- System property openai.api.key

For the OSGI configuration there are the following configurations:

| Configuration Key        | Description                                                                                                                                                                                            | Default Value          |
|--------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------|
| disabled                 | Disable the GPT Chat Completion Service                                                                                                                                                                | false                  |
| chatCompletionUrl        | URL of the chat completion service. Optional, if not OpenAI's default                                                                                                                                  |                        |
| openAiApiKey             | OpenAI API Key from https://platform.openai.com/. If not given, checks key file, environment Variable OPENAI_API_KEY, etc.                                                                             |                        |
| openAiOrganizationId     | Optionally, OpenAI Organization ID from https://platform.openai.com/account/organization.                                                                                                              |                        |
| openAiApiKeyFile         | OpenAI API Key File containing the API key, as an alternative to Open AI Key configuration                                                                                                             |                        |
| defaultModel             | Default model to use for the chat completion. Consider varying prices https://openai.com/pricing                                                                                                       | gpt-3.5-turbo          |
| imageModel               | Optional, a model that is used if an image is given as input, e.g. gpt-4o. If not given, that is rejected.                                                                                        |                        |
| temperature              | Optional temperature setting that determines variability vs. creativity as a floating point between 0.0 and 1.0                                                                                        |                        |
| maximumTokensPerRequest  | If > 0 limit to the maximum number of tokens per request. That's about twice the word count. Caution: Compare with the pricing - on GPT-4 models a thousand tokens might cost $0.01 or more.           | 50000                  |
| maximumTokensPerResponse | Maximum number of tokens to return in the response. Must not exceed the capabilities of the model - as of 10/03/24 this is 4096 for most OpenAI models - which is the default, so no need to set that. | 4096                   |
| connectionTimeout        | Connection timeout in seconds                                                                                                                                                                          | 20                     |
| requestTimeout           | Request timeout in seconds                                                                                                                                                                             | 60                     |
| requestsPerMinute        | Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default                                                                    | 100                    |
| requestsPerHour          | Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default                                                                      | 1000                   |
| requestsPerDay           | Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default                                                                       | 3000                   |
| embeddingsUrl            | URL of the embeddings service. Optional, if not OpenAI's default                                                                                                                                       |                        |
| embeddingsModel          | Optional model to use for the embeddings. The default is.                                                                                                                                              | text-embedding-3-small |

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
content page should have a simple structure and the prompts should be as components in exactly one container containing
components with the prompts. Usable for the prompt components are e.g. teasers or other components that have:

1. a title property (`jcr:title`, `title` or `subtitle`) that is taken as title of the prompt, and
2. a text property (`jcr:description`, `description` or `text`) that is taken as the text of the prompt.

If you want to adapt the default prompt libraries  
`/conf/composum-ai/settings/dialogs/contentcreation/predefinedprompts`
`/conf/composum-ai/settings/dialogs/sidepanel-ai/predefinedprompts`
: these can be copied into the site or some other place and edited there. They do, however, use components and page 
template of the WKND site, so you might need to change the components and page templates to match your site.

If the paths to the prompt libraries contain language specific subpages (`en`, `de` and so forth), the prompt library
in the language of the site is taken, falling back to `en`.

## AI Permission Configuration

<div style="float: right; margin-left: 20px;">
   <a href="../image/ai/configuration/AIPermissionConfiguration.png">
    <img src="../image/ai/configuration/AIPermissionConfiguration.png" alt="Sidebar AI" width="400" />
  </a>
</div>

For the permission configuration we use sets of configuration entries that each allow the use of some of the AI
services (dialogs) in some context, and are additional - the corresponding AI dialogs are accessible if there is one
configuration to allow it. There is however a fallback hierarchy for the permission configuration, where the
fallback is only done if *there is not a single visible entry* in the fallback level:

- Sling Context Aware Configuration
- an OSGI configuration factory at "Composum AI Permission Configuration"
- a single OSGI configuration at "Composum AI Permission Configuration" that is the default configuration when
  nothing else is found, and normally allows all services for all users etc. if not changed.

Thus, if the context aware configuration is not used and the factory is not used either, then the default "all allowing"
configuration is used, which it is ignored otherwise.
If using context aware configuration it's sensible to provide a default configuration at e.g.
/conf/global/sling:configs/com.composum.ai.backend.slingbase.model.GPTPermissionConfiguration/entry1
that forbids everything for sites where sling context aware configuration is not configured as a default and thus
deactivates the other fallback levels (OSGI) generally, and then add configurations for the parts where it is allowed.

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

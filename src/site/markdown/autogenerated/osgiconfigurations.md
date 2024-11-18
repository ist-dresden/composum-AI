# OSGi Configurations

This is an automatically generated overview.

<a name="osgi.ApproximateMarkdownServiceImpl"></a>
# Composum AI Approximate Markdown Service Configuration (slingbase)

Configuration for the Approximate Markdown Service used to get a text representation of a page or component for use with the AI.

| id                        | name                          | type                | default value                                                                 | description                                                                                                                                                                                                                       |
|----|------|------|---------------|-------------|
| urlSourceWhitelist | URL Source Whitelist Regex | String[] |  | Only if using URLs as external source: Whitelist for URLs that can be read and turned into markdown. If not set, reading URLs is turned off. For security reasons you might want to prevent local addresses to be contacted. To allow everything you might use https?://.* , but make sure you have a good blacklist in that case. |
| urlSourceBlacklist | URL Source Blacklist Regex | String[] |  | Only if using URLs as external source: Blacklist for URLs that can be read and turned into markdown. Has precendence over whitelist. |
| textAttributes | Text Attributes | String[] |  | List of attributes that are treated as text and converted to markdown. If not present, no attributes are treated as text. |
| labelledAttributePatternAllow | Labeled Attribute Pattern Allow | String[] |  | Regular expressions for attributes that are output with a label. If not present, none will be output except the text attributes. |
| labelledAttributePatternDeny | Labeled Attribute Pattern Deny | String[] |  | Regular expressions for attributes that are not output with a label. Takes precedence over the corresponding allow regexp list. |
| labelledAttributeOrder | Labelled Attribute Order | String[] |  | List of labelled attributes that come first if they are present, in the given order. |

<a name="osgi.AutoTranslateConfig"></a>
## Composum AI Autotranslate Configuration (aem/core)

Configuration of the automatic translation of AEM pages. The OSGI configuration is only used if no Sling CAConfig configuration is found.

| id                                      | name                                      | type    | default value | description                                                                                                                                                                                                                       |
|-----------------------------------------|-------------------------------------------|---------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| pocUiEnabled                            | Debugging UI                             | boolean | false         | Enable the Autotranslate debugging UI at at /apps/composum-ai/components/autotranslate/list/list.html, normally disabled. Only read from OSGI configuration.                                                                     |
| disabled                                | Disable                                  | boolean | false         | Disable the Autotranslate service.                                                                                                                                                                                              |
| deniedResourceTypes                     | Denied Resource Types                    | String[]| []            | Regexes for denied Resource Types - if the sling:resourceType matches that, then no attributes or child nodes are touched by the automatic translation.                                                                          |
| allowedAttributeRegexes                 | Allowed Additional Attributes             | String[]| []            | Matches for Attributes that are explicitly allowed to be translated, in addition to standard attributes and heuristically recognized attributes. The heuristics is that the value has to have letters and whitespaces. Syntax: regular expressions that match resource type % attribute name - e.g. myapp/component/html%markup |
| deniedAttributesRegexes                 | Denied Attributes                        | String[]| []            | Attributes that are explicitly denied to be translated. Can be used to override the heuristics / standard attributes. Same syntax as allowed attributes.                                                                           |
| ignoreAssetsExceptContentFragments      | Ignore Assets except Content Fragments    | boolean | true          | If true, assets are ignored for translation, except for content fragments. (Otherwise the translator would translate metadata of images and videos.)                                                                               |
| useHighIntelligenceModel                | Use High Intelligence Model               | boolean | true          | If true, the translator will use the 'high-intelligence model' (see OpenAI config) for translation. Default: true.                                                                                                              |
| includeFullPageInRetranslation          | Include Full Page during Retranslation    | boolean | true          | If true we do not only provide changed texts to the AI during re-translating a page with some changes, but give the entire page to provide better context. That is a bit slower and a bit more expensive, but likely improves the result. |
| includeExistingTranslationsInRetranslation| Include Existing Translations in Retranslation| boolean | true          | If true, when retranslating a page with some changes we provide the existing translations of that page to the AI as well as additional context with examples. That is a bit slower and a bit more expensive, but likely improves the result. |

<a name="osgi.GPTChatCompletionServiceImpl"></a>
## Composum AI OpenAI Configuration (backend/base)

Provides rather low level access to the GPT chat completion - use the other services for more specific services.

| id                          | name                                      | type    | default value | description                                                                                                                                                                                                 |
|----------------------------|-------------------------------------------|--------|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| disabled                    | Disable                                   | boolean | false         | Disable the GPT Chat Completion Service                                                                                                                                                                   |
| chatCompletionUrl          | URL of the chat completion service        | String  |               | Optional, if not OpenAI's default https://api.openai.com/v1/chat/completions                                                                                                                            |
| openAiApiKey               | OpenAI API key                            | String  |               | OpenAI API key from https://platform.openai.com/. If not given, we check the key file, the environment Variable OPENAI_API_KEY, and the system property openai.api.key .                                 |
| openAiOrganizationId       | OpenAI Organization ID                    | String  |               | Optionally, OpenAI Organization ID from https://platform.openai.com/account/organization .                                                                                                               |
| openAiApiKeyFile          | OpenAI API key file                       | String  |               | Key File containing the API key, as an alternative to Open AI Key configuration and the variants described there.                                                                                         |
| defaultModel               | Default model                             | String  | gpt-4o-mini   | Default model to use for the chat completion. The default if not set is gpt-4o-mini. Please consider the varying prices https://openai.com/pricing .                                                      |
| highIntelligenceModel      | High intelligence model                   | String  | gpt-4o        | The model that is used for requests that need more reasoning performance. The default if not set is gpt-4o. Please consider the varying prices https://openai.com/pricing .                               |
| imageModel                 | Vision model                              | String  | gpt-4o        | Optional, a model that is used if an image is given as input, e.g. gpt-4o. If not given, image recognition is rejected.                                                                                   |
| temperature                 | Temperature                               | String  |               | Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0                                                                                                                                 |
| seed                       | seed                                      | String  |               | If specified, OpenAI will make a best effort to sample deterministically, such that repeated requests with the same seed and parameters should return the same result.                                     |
| maximumTokensPerRequest    | Maximum Tokens per Request                | int     | 50000         | If > 0 limit to the maximum number of tokens per request. That's about a twice the word count. Caution: Compare with the pricing - on GPT-4 models a thousand tokens might cost $0.01 or more.            |
| maximumTokensPerResponse    | Maximum output tokens per request         | int     | 4096          | Maximum number of tokens to return in the response. Must not exceed the capabilities of the model - as of 10/03/24 this is 4096 for most OpenAI models - which is the default, so no need to set that.     |
| connectionTimeout          | Connection timeout in seconds             | int     | 30            | Default 30                                                                                                                                                                                                 |
| requestTimeout             | Request timeout in seconds                | int     | 300           | Default 300                                                                                                                                                                                                |
| requestsPerMinute           | Maximum requests per minute               | int    | 100                                | Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default 100                                                                                       |
| requestsPerHour             | Maximum requests per hour                 | int    | 1000                               | Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default 1000                                                                                       |
| requestsPerDay              | Maximum requests per day                  | int    | 3000                               | Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default 3000                                                                                       |
| embeddingsUrl               | URL of the embeddings service             | String | https://api.openai.com/v1/embeddings | Optional, if not OpenAI's default https://api.openai.com/v1/embeddings                                                                                                                                                       |
| embeddingsModel            | Embeddings model                          | String  | text-embedding-3-small | Optional model to use for the embeddings. The default is text-embedding-3-small.                                                                                                                        |

<a name="osgi.GPTDictationServiceImpl"></a>
## GPT Dictation Service Configuration (base)

Configures whether it's enabled (default false), the model and the request counts, and the maximum request size.

| id                     | name                               | type | default value | description                                                                                       |
|-----------------------|------------------------------------|------|---------------|---------------------------------------------------------------------------------------------------|
| disabled               | Disabled                           | boolean | false         | Whether the service is disabled.                                                                   |
| model                  | Model                              | String | whisper-1     | The model to use for dictation, default whisper-1.                                               |
| requestsPerMinute      | Maximum requests per minute        | int    | 30            | Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default 30. |
| requestsPerHour        | Maximum requests per hour          | int    | 100           | Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default 100. |
| requestsPerDay         | Maximum requests per day           | int    | 300           | Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default 300. |
| maxRequestSize         | Maximum request size in bytes      | int    | 5000000       | Maximum request size in bytes, default 5000000. (about one minute of stereo audio with 44.1 kHz and 16 bit) |

<a name="osgi.GPTPermissionConfiguration"></a>
## Composum AI Permission Configuration (slingbase)

A configuration for allowed AI services. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| 10 | Services | String[] | [CATEGORIZE, CREATE, SIDEPANEL, TRANSLATE] | List of services to which this configuration applies. Possible values are: CATEGORIZE, CREATE, SIDEPANEL, TRANSLATE. For AEM only create and sidepanel are supported. |
| 20 | Allowed Users | String[] | [.*] | Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration. |
| 30 | Denied Users | String[] |  | Regular expressions for denied users or user groups. Takes precedence over allowed users. |
| 40 | Allowed Paths | String[] | [/content/.*] | Regular expressions for allowed content paths. If not present, no paths are allowed. |
| 50 | Denied Paths | String[] | [/content/dam/.*] | Regular expressions for denied content paths. Takes precedence over allowed paths. |
| 60 | Allowed Views | String[] | [.*] | Regular expressions for allowed views - that is, for URLs like /editor.html/.*. If not present, no views are allowed. Use .* to allow all views. |
| 70 | Denied Views | String[] |  | Regular expressions for denied views. Takes precedence over allowed views. |
| 80 | Allowed Components | String[] | [.*] | Regular expressions for allowed resource types of components. If not present, no components are allowed. |
| 90 | Denied Components | String[] |  | Regular expressions for denied resource types of components. Takes precedence over allowed components. |
| 100 | Allowed Page Templates | String[] | [.*] | Regular expressions for allowed page templates. If not present, all page templates are allowed. |
| 110 | Denied Page Templates | String[] |  | Regular expressions for denied page templates. Takes precedence over allowed page templates. |

<a name="osgi.GPTPromptLibrary"></a>
# Composum AI Prompt Library Configuration (backend/slingbase)

Location for the prompt library for Composum AI. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| id                        | name                           | type   | default value | description                                                                                      |
|---------------------------|--------------------------------|--------|---------------|--------------------------------------------------------------------------------------------------|
| contentCreationPromptsPath | Content Creation Prompts Path  | String |               | Path to the content creation prompts. Either a JSON file, or a page.                            |
| sidePanelPromptsPath       | Side Panel Prompts Path        | String |               | Path to the side panel prompts. Either a JSON file, or a page.                                  |

<a name="osgi.GPTTranslationServiceImpl"></a>
## Composum AI Translation Service Configuration (backend/base)

Configuration for the basic Composum AI Translation Service

| id                | name                                                                 | type    | default value | description                                                                                                                                                                                                                       |
|-------------------|----------------------------------------------------------------------|---------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| disabled          | Disable the translation service                                      | boolean | false         |  |
| fakeTranslation    | Fake translation                                                     | boolean | false         | For quick and inexpensive testing, when you just want to check that the translation does something for e.g. a bulk of texts, you can enable this. The "translation" then just turns the text iNtO tHiS cApItAlIsAtIoN. Easy to spot, but probably doesn't destroy the content completely. |
| diskCache         | Disk cache                                                          | String  |               | Path to a directory where to cache the translations. If empty, no caching is done. If the path is relative, it is relative to the current working directory. If the path is absolute, it is used as is.                          |
| temperature       | temperature                                                         | String  |               | The sampling temperature, between 0 and 1. Higher values like 0.8 will make the output more random, while lower values like 0.2 will make it more focused and deterministic.                                                      |
| seed              | seed                                                                | String  |               | If specified, OpenAI will make a best effort to sample deterministically, such that repeated requests with the same seed and parameters should return the same result.                                                             |

<a name="osgi.GetPageMarkdownAITool"></a>
# Composum AI Tool Get Page Markdown (backend/slingbase)

Provides the AI with a tool to search for page paths. Needs a lucene index for all pages. If there is no configuration, the tool is not active.

| id                     | name                     | type   | default value | description                                                                 |
|-----------------------|--------------------------|--------|---------------|-----------------------------------------------------------------------------|
| allowedPathsRegex     | Allowed paths regex      | String | /content/.*   | A regex to match the paths that this tool is allowed to be used on. Default: /content/.* |

<a name="osgi.HtmlToApproximateMarkdownServicePlugin"></a>
## Composum AI Html To Approximate Markdown Service Plugin (slingbase)

A plugin for the ApproximateMarkdownService that transforms the rendered HTML of components to markdown, which can work better than trying to guess the text content from the JCR representation (as is the default) but probably doesn't work for all components. So it can be enabled for some sling resource types by regex. We will not use this for the first two levels below the page, as that could include unwanted stuff like headers and footers.

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| allowedResourceTypes | Allowed resource types | String[] | {".*"} | Regular expressions for allowed resource types. If not present, no resource types are allowed. |
| deniedResourceTypes | Denied resource types | String[] | {} | Regular expressions for denied resource types. Takes precedence over allowed resource types. |

<a name="osgi.MarkdownSlingCacheImpl"></a>
## Composum AI Approximate Markdown Cache Service Configuration (slingbase)

If configured, caches the calculated approximate markdown of pages.  
CAUTION: the page content must be independent of the user, or you might leak one user's data to another!

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| disabled | Disable | boolean | false | Disable the service |
| cacheRootPath | Cache Root Path | String | | The JCR root path where the markdown is stored. If not set, no caching is done. Suggestion: /var/composum/ai-markdown-cache. To set this up you'll need to create this path in the repository, add a service user for this bundle's name (composum-ai-integration-backend-slingbase) and make the path writeable for this user. |

<a name="osgi.SearchPageAITool"></a>
## Composum AI Tool Search Pages (backend/slingbase)

Provides the AI with a tool to search for page paths. Needs a lucene index for all pages. If there is no configuration the tool is not active.

| id          | name          | type | default value | description                                                                                     |
|-------------|---------------|------|---------------|-------------------------------------------------------------------------------------------------|
| resultCount | Result count  | int  | 20            | The number of results to return. Default is 20.                                               |
| siteLevel   | Site level    | int  | 2             | The number of path segments a site has, used to identify the site root. Default is 2, for sites like /content/my-site. |

<a name="osgi.SlingCaConfigPluginImpl"></a>
### Composum AI SlingCaConfig Plugin (slingbase)

Allows enabling / disabling the Sling Context Aware Configuration of the Composum AI.

| id | name   | type    | default value | description                                                |
|----|--------|---------|---------------|------------------------------------------------------------|
| 1  | Enabled| boolean | true          | Whether the Sling Context Aware Configuration of the Composum AI is enabled. |


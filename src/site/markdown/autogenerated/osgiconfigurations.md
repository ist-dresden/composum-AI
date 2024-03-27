# OSGi Configurations

This is an automatically generated overview.

<a name="osgi.ApproximateMarkdownServiceImpl"></a>
## Composum AI Approximate Markdown Service Configuration (slingbase)

Configuration for the Approximate Markdown Service used to get a text representation of a page or component for use with the AI.

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| 1 | Text Attributes | String[] |  | List of attributes that are treated as text and converted to markdown. If not present, no attributes are treated as text. |
| 2 | Labeled Attribute Pattern Allow | String[] |  | Regular expressions for attributes that are output with a label. If not present, none will be output except the text attributes. |
| 3 | Labeled Attribute Pattern Deny | String[] |  | Regular expressions for attributes that are not output with a label. Takes precedence over the corresponding allow regexp list. |
| 4 | Labelled Attribute Order | String[] |  | List of labelled attributes that come first if they are present, in the given order. |

<a name="osgi.AutoTranslateConfig"></a>
## Composum AI Autotranslate Configuration (aem-core)

Configuration of the automatic translation of AEM pages. The OSGI configuration is only used if no Sling CAConfig configuration is found. Proof of concept quality - give it a try. :-)

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| pocUiEnabled | Proof of concept UI | boolean | false | Enable the Autotranslate proof of concept UI at /apps/composum-ai/components/autotranslate/list/list.html , normally disabled. Only read from OSGI configuration. |
| disabled | Disable the Autotranslate service | boolean | false |  |
| deniedResourceTypes | Denied Resource Types | String[] |  | Regexes for denied Resource Types - if the sling:resourceType matches that, then no attributes or child nodes are touched by the automatic translation. |
| allowedAttributeRegexes | Allowed Additional Attributes | String[] |  | Matches for Attributes that are explicitly allowed to be translated, in addition to standard attributes and heuristically recognized attributes. The heuristics is that the value has to have letters and whitespaces. Syntax: regular expressions that match resource type % attribute name - e.g. myapp/component/html%markup |
| deniedAttributesRegexes | Denied Attributes | String[] |  | Attributes that are explicitly denied to be translated. Can be used to override the heuristics / standard attributes. Same syntax as allowed attributes. |
| ignoreAssetsExceptContentFragments | Ignore Assets except Content Fragments | boolean | true | If true, assets are ignored for translation, except for content fragments. (Otherwise the translator would translate metadata of images and videos.) |

<a name="osgi.GPTChatCompletionServiceImpl"></a>
## GPTChatCompletionServiceConfig (backend-base)

Provides rather low level access to the GPT chat completion - use the other services for more specific services.

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| disabled | Disable | boolean | false | Disable the GPT Chat Completion Service |
| chatCompletionUrl | URL of the chat completion service | String |  | Optional, if not OpenAI's default https://api.openai.com/v1/chat/completions |
| openAiApiKey | OpenAI API key | String |  | OpenAI API key from https://platform.openai.com/. If not given, we check the key file, the environment Variable OPENAI_API_KEY, and the system property openai.api.key . |
| openAiOrganizationId | OpenAI Organization ID | String |  | Optionally, OpenAI Organization ID from https://platform.openai.com/account/organization . |
| openAiApiKeyFile | OpenAI API key file | String |  | Key File containing the API key, as an alternative to Open AKI Key configuration and the variants described there. |
| defaultModel | Default model | String | gpt-3.5-turbo | Default model to use for the chat completion. The default if not set is gpt-3.5-turbo. Please consider the varying prices https://openai.com/pricing . |
| imageModel | Vision model | String | gpt-4-vision-preview | Optional, a model that is used if an image is given as input, e.g. gpt-4-vision-preview. If not given, image recognition is rejected. |
| temperature | Temperature | String |  | Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0 |
| maximumTokensPerRequest | Maximum Tokens per Request | int | 50000 | If > 0 limit to the maximum number of tokens per request. That's about a twice the word count. Caution: Compare with the pricing - on GPT-4 models a thousand tokens might cost $0.01 or more. |
| maximumTokensPerResponse | Maximum output tokens per request | int | 4096 | Maximum number of tokens to return in the response. Must not exceed the capabilities of the model - as of 10/03/24 this is 4096 for most OpenAI models - which is the default, so no need to set that. |
| connectionTimeout | Connection timeout in seconds | int | 20 | Default 20 |
| requestTimeout | Request timeout in seconds | int | 120 | Default 120 |
| requestsPerMinute | Maximum requests per minute | int | 100 | Maximum count of requests to ChatGPT per minute - from the second half there will be a slowdown to avoid hitting the limit. Default 100 |
| requestsPerHour | Maximum requests per hour | int | 1000 | Maximum count of requests to ChatGPT per hour - from the second half there will be a slowdown to avoid hitting the limit. Default 1000 |
| requestsPerDay | Maximum requests per day | int | 3000 | Maximum count of requests to ChatGPT per day - from the second half there will be a slowdown to avoid hitting the limit. Default 3000 |

<a name="osgi.GPTPermissionConfiguration"></a>
## Composum AI Permission Configuration (slingbase)

A configuration for allowed AI services. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| ID | Name | Type | Default Value | Description |
|----|------|------|---------------|-------------|
| 10 | Services | String[] | [CATEGORIZE, CREATE, SIDEPANEL, TRANSLATE] | List of services to which this configuration applies. Possible values are: CATEGORIZE, CREATE, SIDEPANEL, TRANSLATE. For AEM only create and sidepanel are supported. |
| 20 | Allowed Users | String[] | [.*] | Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration. |
| 30 | Denied Users | String[] |  | Regular expressions for denied users or user groups. Takes precedence over allowed users. |
| 40 | Allowed Paths | String[] | [/content/.*] | Regular expressions for allowed content paths. If not present, no paths are allowed. |
| 50 | Denied Paths | String[] | [/content/dam/.*] | Regular expressions for denied content paths. Takes precedence over allowed paths. Content fragments are not allowed by default since there has been trouble in the UI. |
| 60 | Allowed Views | String[] | [.*] | Regular expressions for allowed views - that is, for URLs like /editor.html/.*. If not present, no views are allowed. Use .* to allow all views. |
| 70 | Denied Views | String[] |  | Regular expressions for denied views. Takes precedence over allowed views. |
| 80 | Allowed Components | String[] | [.*] | Regular expressions for allowed resource types of components. If not present, no components are allowed. |
| 90 | Denied Components | String[] |  | Regular expressions for denied resource types of components. Takes precedence over allowed components. |
| 100 | Allowed Page Templates | String[] | [.*] | Regular expressions for allowed page templates. If not present, all page templates are allowed. |
| 110 | Denied Page Templates | String[] |  | Regular expressions for denied page templates. Takes precedence over allowed page templates. |

<a name="osgi.GPTPromptLibrary"></a>
## Composum AI Prompt Library Configuration (slingbase)

Location for the prompt library for Composum AI. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| contentCreationPromptsPath | Content Creation Prompts Path | String |  | Path to the content creation prompts. Either a JSON file, or a page. |
| sidePanelPromptsPath | Side Panel Prompts Path | String |  | Path to the side panel prompts. Either a JSON file, or a page. |

<a name="osgi.GPTTranslationServiceImpl"></a>
## Composum AI Translation Service Configuration (backend)

Configuration for the basic Composum AI Translation Service

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| disabled | Disable the translation service | boolean | false | Disable the translation service |
| fakeTranslation | Fake translation | boolean | false | For quick and inexpensive testing, when you just want to check that the translation does something for e.g. a bulk of texts, you can enable this. The "translation" then just turns the text iNtO tHiS cApItAlIsAtIoN. Easy to spot, but probably doesn't destroy the content completely. |
| diskCache | Disk cache | String | "" | Path to a directory where to cache the translations. If empty, no caching is done. If the path is relative, it is relative to the current working directory. If the path is absolute, it is used as is. |

<a name="osgi.HtmlToApproximateMarkdownServicePlugin"></a>
## Composum AI Html To Approximate Markdown Service Plugin (slingbase)

A plugin for the ApproximateMarkdownService that transforms the rendered HTML of components to markdown, which can work better than trying to guess the text content from the JCR representation (as is the default) but probably doesn't work for all components. So it can be enabled for some sling resource types by regex. We will not use this for the first two levels below the page, as that could include unwanted stuff like headers and footers.

| id | name | type | default value | description |
|----|------|------|---------------|-------------|
| Allowed resource types | allowedResourceTypes | String[] | {".*"} | Regular expressions for allowed resource types. If not present, no resource types are allowed. |
| Denied resource types | deniedResourceTypes | String[] | {} | Regular expressions for denied resource types. Takes precedence over allowed resource types. |

<a name="osgi.SlingCaConfigPluginImpl"></a>
## Composum AI SlingCaConfig Plugin (slingbase)

Allows enabling / disabling the Sling Context Aware Configuration of the Composum AI.

| id | name   | type    | default value | description                                                |
|----|--------|---------|---------------|------------------------------------------------------------|
| 1  | Enabled| Boolean | true          | Whether the Sling Context Aware Configuration of the Composum AI is enabled. |

# Sling Context Aware Configurations

This is an automatically generated overview.

<a name="slingca.AutoTranslateCaConfig"></a>
## Composum AI Automatic Translation Configuration (aem/core)

Configures rollout details for automatic translation.

| id                                         | label                                               | type   | default value | description                                                                                                                                                                                                                                           |
|--------------------------------------------|------------------------------------------------|---------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| additionalInstructions                     | Additional Instructions (Deprecated)                | String |               | Additional instructions for the automatic translation. Deprecated, please use 'Rules for additional Instructions' instead - if you do not give a path regex nor a content pattern the instructions will be used everywhere.                        |
| rules                                      | Rules for additional Instructions                    | AutoTranslateRuleConfig[] |               | Rules that give additional instructions for translation if certain words or phrases are present in the page.                                                                                                                                      |
| translationTables                          | Translation Tables                                  | AutoTranslateTranslationTableConfig[] |               | Translation tables for the automatic translation - XLS or CSV files of terms and their translations. This is an alternative to translation rules if there are many 'Translate X as Y' rules.                                                      |
| preferHighIntelligenceModel                | Prefer High Intelligence Model                 | boolean |               | If set, the high intelligence model will be used for translation.                                                                                                                                                                                                                                                                                                 |
| preferStandardModel                        | Prefer Standard Model                          | boolean |               | If set, the standard model will be used for translation. Opposite of 'Prefer High Intelligence Model'.                                                                                                                                                                                                                                                            |
| includeFullPageInRetranslation            | Include Full Page during Retranslation              | String |               | If true we do not only provide changed texts to the AI during re-translating a page with some changes, but give the entire page to provide better context. That is a bit slower and a bit more expensive, but likely improves the result. This overrides the default from OSGI configuration. |
| includeExistingTranslationsInRetranslation | Include Existing Translations in Retranslation      | String |               | If true, when retranslating a page with some changes we provide the existing translations of that page to the AI as well as additional context with examples. That is a bit slower and a bit more expensive, but likely improves the result. This overrides the default from OSGI configuration. |
| comment                                    | Optional Notes (for your documentation, not used by the application) | String |               | An optional comment about the configuration, for documentation purposes (not used by the translation).                                                                                                                                              |
| temperature                                | Temperature                                         | String |               | Optional temperature setting that determines variability and creativity as a floating point between 0.0 and 1.0                                                                                                                                   |
| translationTableRuleText                  | Translation Table Rule Text                         | String |               | Optional pattern to create translation rules from translation tables. {0} is the word in the source language, {1} the word in the target language. If not given we use a default: Translate '{0}' as '{1}'.                                      |

<a name="slingca.AutoTranslateRuleConfig"></a>
## AutoTranslateRuleConfig (aem/core)

A rule to be added to the Composum AI Automatic Translation Configuration with translation instructions for pages matching the rule.

| id                     | label                               | type   | default value | description                                                                                                                                                                                                                       |
|-----------------------|-------------------------------------|--------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| pathRegex             | Path Regex                          | String |               | A regular expression matching the absolute path to the page, incl. jcr:content. E.g. .*/home/products/.* will match all pages under .../home/products/. If empty every page will match if the content pattern condition is met. |
| contentPattern        | Content Pattern                     | String |               | A word or phrase that must be present in the content of the page for the rule to match. E.g. 'Product' will match all pages that contain the word 'Product', case-insensitive. Spaces will also match any whitespace. If empty every page will match if the path condition is met. |
| additionalInstructions | Additional Instructions             | String |               | Additional instructions for the automatic translation in case this rule matches.                                                                                                                                                 |
| comment               | Optional Notes (for your documentation, not used by the application) | String |               | An optional comment for the rule, for documentation purposes (not used by the translation).                                                                                                                                   |

<a name="slingca.AutoTranslateTranslationTableConfig"></a>
## AutoTranslateTranslationTableConfig (aem/core)

Configures a translation table for the automatic translation - an XLS or CSV file of terms and their translations. Properties include the path to the file resource or DAM asset, sheet index, start row, key column, and value column.

| id            | label                                   | type   | default value | description                                                                                                                                                                                                 |
|---------------|-----------------------------------------|--------|---------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| path          | Path to XLS or CSV File                | String |               | The full JCR path to the XLS or CSV file containing the translation table. Can be either a file resource or an AEM asset. E.g.: /content/dam/yourapplication/aitranslation/endedict.xls                     |
| sheetIndex    | Sheet Index                            | int    |               | The index of the sheet in the XLS file containing the translation table. The first sheet is 1. Ignored for CSV files.                                                                                     |
| startRow      | Start Row                              | int    |               | The row in the sheet where the translation table starts. The first row is 1, following Excel conventions.                                                                                                  |
| keyColumn     | Key Column                             | String |               | The column in the sheet containing the keys (terms to be translated). The first column is A (following Excel conventions) or 1.                                                                            |
| valueColumn   | Value Column                           | String |               | The column in the sheet containing the values (translations). The first column is A (following Excel conventions) or 1.                                                                                     |
| comment       | Optional Notes (for your documentation, not used by the application) | String |               | An optional comment for the rule, for documentation purposes (not used by the translation).                                                                                                               |

<a name="slingca.GPTPermissionConfiguration"></a>
## Composum AI Permission Configuration (slingbase)

A configuration for allowed AI services. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| id                  | label                  | type     | default value | description                                                                                                      |
|---------------------|------------------------|----------|---------------|------------------------------------------------------------------------------------------------------------------|
| services            | Services               | String[] | -             | List of services to which this configuration applies. Possible values are: categorize, create, sidepanel, translate. For AEM only create and sidepanel are supported. |
| allowedUsers        | Allowed Users          | String[] | .*            | Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration. |
| deniedUsers         | Denied Users           | String[] | -             | Regular expressions for denied users or user groups. Takes precedence over allowed users.                        |
| allowedPaths        | Allowed Paths          | String[] | /content/.*   | Regular expressions for allowed content paths. If not present, no paths are allowed.                             |
| deniedPaths         | Denied Paths           | String[] | /content/dam/.* | Regular expressions for denied content paths. Takes precedence over allowed paths.                                |
| allowedViews        | Allowed Views          | String[] | .*            | Regular expressions for allowed views - that is, for URLs like /editor.html/.* . If not present, no views are allowed. Use .* to allow all views. |
| deniedViews         | Denied Views           | String[] | -             | Regular expressions for denied views. Takes precedence over allowed views.                                        |
| allowedComponents   | Allowed Components     | String[] | .*            | Regular expressions for allowed resource types of components. If not present, no components are allowed.          |
| deniedComponents    | Denied Components      | String[] | -             | Regular expressions for denied resource types of components. Takes precedence over allowed components.             |
| allowedPageTemplates| Allowed Page Templates | String[] | .*            | Regular expressions for allowed page templates. If not present, all page templates are allowed.                  |
| deniedPageTemplates | Denied Page Templates  | String[] | -             | Regular expressions for denied page templates. Takes precedence over allowed page templates.                      |

<a name="slingca.GPTPromptLibrary"></a>
## Composum AI Prompt Library Configuration (backend/slingbase)

Location for the prompt library for Composum AI. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| id                        | label                           | type   | default value | description                                       |
|---------------------------|---------------------------------|--------|---------------|---------------------------------------------------|
| contentCreationPromptsPath | Content Creation Prompts Path   | String |               | Path to the content creation prompts.             |
| sidePanelPromptsPath       | Side Panel Prompts Path         | String |               | Path to the side panel prompts.                   |


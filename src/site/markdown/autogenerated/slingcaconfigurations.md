# Sling Context Aware Configurations


### Composum AI Automatic Translation POC Rollout Configuration (AutoTranslateLiveActionConfig)

Configures rollout details for automatic translation. This is a demo and not yet fully functional.

| id | label | type | default value | description |
|----|-------|------|---------------|-------------|
| additionalInstructions | Additional Instructions | String |  | Additional instructions for the automatic translation. |

## Composum AI Permission Configuration (slingbase)

A configuration for allowed AI services. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| ID                   | Label                  | Type      | Default Value | Description                                                                                                      |
|----------------------|------------------------|-----------|---------------|------------------------------------------------------------------------------------------------------------------|
| services             | Services               | String[]  | -             | List of services to which this configuration applies. Possible values are: ...                                 |
| allowedUsers         | Allowed Users          | String[]  | .*            | Regular expressions for allowed users or user groups. If not present, no user is allowed from this configuration. |
| deniedUsers          | Denied Users           | String[]  | -             | Regular expressions for denied users or user groups. Takes precedence over allowed users.                        |
| allowedPaths         | Allowed Paths           | String[]  | /content/.*  | Regular expressions for allowed content paths. If not present, no paths are allowed.                             |
| deniedPaths          | Denied Paths            | String[]  | /content/dam/.* | Regular expressions for denied content paths. Takes precedence over allowed paths.                               |
| allowedViews         | Allowed Views           | String[]  | .*            | Regular expressions for allowed views - that is, for URLs like /editor.html/.* . If not present, no views are allowed. Use .* to allow all views. |
| deniedViews          | Denied Views            | String[]  | -             | Regular expressions for denied views. Takes precedence over allowed views.                                       |
| allowedComponents    | Allowed Components      | String[]  | .*            | Regular expressions for allowed resource types of components. If not present, no components are allowed.         |
| deniedComponents     | Denied Components       | String[]  | -             | Regular expressions for denied resource types of components. Takes precedence over allowed components.            |
| allowedPageTemplates | Allowed Page Templates  | String[]  | .*            | Regular expressions for allowed page templates. If not present, all page templates are allowed.                  |
| deniedPageTemplates  | Denied Page Templates   | String[]  | -             | Regular expressions for denied page templates. Takes precedence over allowed page templates.                     |

## Composum AI Prompt Library Configuration (Composum-AI)

Location for the prompt library for Composum AI. There can be multiple configurations, and the allowed services are aggregated.
There is a fallback configuration that is used if no other configuration is found, and a factory for multiple configurations which override the fallback configuration if present.
If configured, Sling Context Aware Configuration takes precedence over OSGI configuration.

| id                        | label                           | type   | default value | description                                                |
|---------------------------|---------------------------------|--------|---------------|------------------------------------------------------------|
| contentCreationPromptsPath | Content Creation Prompts Path   | String |               | Path to the content creation prompts. Either a JSON file, or a page. |
| sidePanelPromptsPath       | Side Panel Prompts Path         | String |               | Path to the side panel prompts. Either a JSON file, or a page. |

## Composum AI OpenAI Configuration (slingbase)

Configurations for the OpenAI backend for Composum AI

| id              | label              | type   | default value | description                                                                                                                             |
|-----------------|--------------------|--------|---------------|-----------------------------------------------------------------------------------------------------------------------------------------|
| openAiApiKey    | OpenAI API Key     | String |               | OpenAI API Key from https://platform.openai.com/. If not given, this falls back to the OSGI configuration, the environment Variable OPENAI_API_KEY, and the system property openai.api.key . |
| openAiOrganizationId | OpenAI Organization ID | String |               | Optionally, OpenAI Organization ID from https://platform.openai.com/.                                                                   |
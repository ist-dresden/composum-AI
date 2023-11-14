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

## OpenAI API Key and general configuration

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
- OSGI configuration at "Composum AI GPT Chat Completion Service" / "Composum AI OpenAI Configuration"
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

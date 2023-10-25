# Configuration of Composum AI

Status: partially specified and implemented - only the API key

## Basic idea

We need a way to configure the application for several points:

- the API key for OpenAI
- the rate limiting
- the prompt library

There should be a basic configuration in the OSGI (or a default contained in the application itself, if applicable),
but all these configurations can be site-specific, or even user-specific. So it's more appropriate to have a
configuration in the JCR repository that is inherited from parent pages and takes precedence over OSGI configuration
if present.

As a first step, we will only implement the API key configuration.

## Basic implementation decisions

We will use Sling Context Aware Configuration for that. There will be @interface configuration classes for that using
@Configuration . There will be separate configuration classes for the API key, the rate limiting and the prompt library.

## Not in scope

As a first step, we currently will not implement configuring the rate limiting and the prompt library. The aim of the
design in this document is, however, to make it easy to add these later, without changing the basic design.
We currently do not implement user specific configuration, version control, environment-based configuration,
configuration auditing, configuration validation.

## Implementation details

### Configuration in the JCR, using sling context aware configuration

package com.composum.ai.backend.slingbase.model;

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

@Configuration(label = "Composum AI OpenAI Configuration", description = "Configurations for the OpenAI backend for
Composum AI")
// is also added to Sling-ContextAware-Configuration-Classes bnd header in pom.xml
public @interface OpenAIConfig {

    @Property(label = "OpenAI API Key", description = "OpenAI API Key from https://platform.openai.com/. If not given, this falls back to the OSGI configuration, the environment Variable OPENAI_API_KEY, and the system property openai.api.key .")
    String openAiApiKey();

}

We have a fallback hierarchy for the OpenAI API key:

- Sling Context Aware Configuration
- OSGI configuration at "Composum AI GPT Chat Completion Service" - this is configured by default for AEMaaCS
  as $[secret:OPENAI_API_KEY] to retrieve a value configurable in the cloud manager
- Environment variable OPENAI_API_KEY

For the sling context aware configuration, we have a configuration
com.composum.ai.backend.slingbase.model.OpenAIConfig with the property openAiApiKey . Thus, set up configuration of
the API key in the JCR repository has the following possibilities:

- to enable it globally (even if you do not normally use Sling Context Aware Configuration), you could set up a
  configuration node at
  /conf/global/sling:configs/com.composum.ai.backend.slingbase.model.OpenAIConfig
  with the property openAiApiKey set to the OpenAI API key. Caution: this requires read permissions for the users.
- to enable it for a site or page tree that already has a sling:configRef set up (searchable with query
  /content//*[@sling:configRef]) you could set up a configuration node
  com.composum.ai.backend.slingbase.model.OpenAIConfig in
  the corresponding /conf/.../sling:configs node.
- otherwise you'd have to set up Sling Context Aware Configuration for the page tree in question.

## References

- [Sling Context Aware Configuration](https://sling.apache.org/documentation/bundles/context-aware-configuration/context-aware-configuration.html)
- [Secrets in the AEMaaCS cloud](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/deploying/configuring-osgi.html?lang=en)

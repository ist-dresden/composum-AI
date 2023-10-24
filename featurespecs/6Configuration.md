# Configuration of Composum AI

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

We will discuss the structure of storing the configuration in the JCR and the model classes for that.

### Configuration in the JCR, using sling context aware configuration

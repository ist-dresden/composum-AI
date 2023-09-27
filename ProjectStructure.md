# Project structure (Planned)

## backend : general "under the hood" services

- base : contains the basic strategies for ChatGPT API access and services building on that, and is intended to be
  agnostic to the underlying platform - that is, no Apache Sling dependencies.

## composum : everything that's needed for integration into Composum Pages

- bundle : OSGI bundle providing the code to integrate with Composum Pages . Mostly a thin Sling servlet wrapper around
  backend base services.
- package : /libs content for integration with Composum Pages : components, javascript ; deploys base and bundle

## aem

- aem/ui.core : OSGI bundle providing the code to integrate the backend services with AEM
- aem/ui.apps : /libs content for integration with AEM editor
- aem/all : A single "uber"-package deploying everything necessary for integration into AEM

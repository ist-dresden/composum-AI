# Project structure

## backend/base : general "under the hood" services

Contains the basic strategies for ChatGPT API access and services building on that, and is intended to be
agnostic to the underlying platform - that is, no Apache Sling dependencies.

## backend/slingbase

Contains parts of the implementation that are specific to Apaches Sling and thus relevant for both the Composum and
AEM variant of the application - servlets, JCR specific services, OSGI / SlingCA configuration.

## composum : everything that's needed for integration into Composum Pages

- bundle : OSGI bundle providing the code to integrate with Composum Pages . Mostly a thin Sling servlet wrapper around
  backend base services.
- package : /libs content for integration with Composum Pages : components, javascript ; deploys base and bundle

## aem

The structure is based on the
[AEM archetype](https://experienceleague.adobe.com/docs/experience-manager-core-components/using/developing/archetype/overview.html?lang=en)
version 37. Among the modules are:

- aem/ui.core : OSGI bundle providing the code to integrate the backend services with AEM
- aem/ui.apps : /libs content for integration with AEM editor
- aem/all : A single "uber"-package deploying everything necessary for integration into AEM

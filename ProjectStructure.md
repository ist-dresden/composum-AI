# Project structure (Planned)

## backend : general "under the hood" services

- base : contains the basic strategies for ChatGPT API access and services building on that, and is intended to be
  agnostic to the underlying platform - that is, no Apache Sling dependencies.
- bundle : an OSGI bundle for the deployment and configuration of the basic module within Apache Sling -
  should be deployable on plain Apache Sling, Composum Platform and AEM

## composum : everything that's needed for integration into Composum Pages

- ui.core : OSGI bundle providing the code to integrate with Composum Pages
- ui.apps : /libs content for integration with Composum Pages
- all : A single "uber"-package deploying everything necessary for integration into Composum Pages

## aem (planned)

- all : A single "uber"-package deploying everything necessary for integration into AEM

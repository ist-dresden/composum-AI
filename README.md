# Composum ChatGPT integration

## Basic idea

With the advent of large language models like ChatGPT, content creation can become considerably easier with the new
support by AI systems. [OpenAI](https://openai.com/) provides a number of rather easy to use API that can be leveraged
in content management systems like [Composum Pages](https://www.composum.com/home.html) or the
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html).
This project provides modules that provide basic backend services and frontend integration into Composum Pages
(as the primary target), possibly later also support for use in AEM, since both share quite some commonality through
being based on the [Apache Sling](https://sling.apache.org/) platform.
Where possible and appropriate, some components will remain platform-agnostic.

## Provided services



## Project structure (Planned)

### backend : general "under the hood" services

- base : contains the basic strategies for ChatGPT API access and services building on that, and is intended to be
  agnostic to the underlying platform - that is, no Apache Sling dependencies.
- bundle : an OSGI bundle for the deployment and configuration of the basic module within Apache Sling -
  should be deployable on plain Apache Sling, Composum Platform and AEM

### composum : everything that's needed for integration into Composum Pages

- ui.core : OSGI bundle providing the code to integrate with Composum Pages
- ui.apps : /libs content for integration with Composum Pages
- all : A single "uber"-package deploying everything necessary for integration into Composum Pages

### aem (planned)

- all : A single "uber"-package deploying everything necessary for integration into AEM

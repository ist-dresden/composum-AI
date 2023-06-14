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

## Current status

In our [first release](https://github.com/ist-dresden/composum-chatgpt-integration/releases/tag/composum-chatgpt
-integration-0.1.1) we have implemented the following dialogs that help with editing in Composum Pages:

- a translation dialog that can be opened on any edited textfield in multilingual websites and suggests translations 
  of the text of a component into other languages
- the text of page descriptions and summaries, keywords, navigation title
- a multi purpose content creation dialog that can support you by creating text for any edited text fields. The text 
  can be created either from a user supplied prompt, or by applying a (user supplied or predefined) ChatGPT prompt 
  to the existing text of the page / component. That way, one could create summaries of the page, or extracts wrt. a 
  topic, overviews, orjust request suggestions to reformulate or expand user supplied text fragments, ... Your 
  imagination is the limit.
- a page category dialog with suggestions for page categories (that are used as SEO keywords)

## Try it out!

The easiest way to try it are the Composum Sling starter (a JAR you can execute locally) or the docker image
[composum/featurelauncher-composum](https://hub.docker.com/r/composum/featurelauncher-composum)
available through the 
[Composum Launcher](https://github.com/ist-dresden/composum-launch) project - see there for description.
You will need an [OpenAI API key](https://platform.openai.com/account/api-keys) secret key to run it, and configure 
that in the [Felix Console configuration tab](http://localhost:8080/system/console/configMgr) at "GPT Chat 
Completion Service".

## Further ideas in investigation

- A dialog that is focused on a providing ChatGPT supported feedback about the page / components etc., which can be 
  used for grammar / spelling checks, reviews, general suggestions etc. 
- possibly support of the user when searching the site using natural language
- possibly support of the user by answering questions about the site content
- possibly augment support AEM with the provided services.

## Development with ChatGPT and Github Copilot

Part of the intention of the project is to evaluate the use of AI services, in particular ChatGPT and the Github 
Copilot IntelliJ plugin, for speeding up development. Some outcomes of that:

- The [feature creation process](featurespecs/FeatureCreationProcess.md) massively improves the quality and speed of 
  specifying a feature and thinking it through. See the [feature specification directory](featurespecs/) for 
  examples of that.
- Speeding up the development is ongoing, but Github Copilot certainly speeds up a lot of things.

## More project documentation

[Next steps](./NextSteps.md) contains a sketch of the next steps to be taken.

[Architecture](./Architecture.md) contains a description of the chosen project architecture and records 
architectural decisions.

[Project structure](./ProjectStructure.md) describes the module structure of the project.

[ChatGPT API Analysis](./ChatGPTAPI.md) contains a discussion of the ChatGPT API wrt. our project.

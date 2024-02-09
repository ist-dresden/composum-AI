# Composum AI - AI-powered content creation integrated in Adobe Experience Manager and Composum Pages

> Fast and flexible,<br>
> Composum empowers you,<br>
> Content blooms with ease.<br>
> -- ChatGPT

We provide several content creation assistants that are tightly integrated into
[Composum Pages](https://www.composum.com/) or
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html) (AEM)
putting the power of AI supported content creation at your fingertips.
For a quick overview - here are two 2.5min demonstrations, one for AEM and one for Composum Pages:

[Click for a 2.5min demonstration video for Adobe AEM](https://github.com/ist-dresden/composum-AI/assets/999184/70b7e6a1-41f2-4dbf-8e5d-7db6bf17233d)

[Click for a 2.5min demonstration video of Composum AI for Composum Pages](https://github.com/ist-dresden/composum-AI/assets/999184/18595f2a-e0b5-49f3-bc4c-65d6a8bc93f6)

## Basic idea

With the advent of large language models (LLM) like [ChatGPT](https://openai.com/blog/chatgpt),
content creation in content management systems like [Composum Pages](https://www.composum.com/home.html) or the
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html)
can become considerably easier with the new support by AI systems.
While [OpenAI](https://openai.com/)'s ChatGPT was and is probably the most noteable forerunner, 
there is quite number of commercial systems like
[Anthropic's Claude](https://www.anthropic.com/index/introducing-claude) (UK),
[Google's Bard](https://bard.google.com/),
[Aleph's Luminous](https://www.aleph-alpha.com/luminous) (Germany) in development, to name only a few. 
All of there provide APIs that allow 
them to be used very flexibly in a lot of ways that are useful for supporting a CMS - for translation, content
creation, review, summarizing, chat, you name it. Optionally it is possible to use the vision capabilities
on images as well, e.g. to generate alt texts for images.

Composum AI provides AI services for [Composum Pages](https://www.composum.com/home.html) and
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html). Both
share quite some commonality through being based on the
[Apache Sling](https://sling.apache.org/) platform.
Where possible and appropriate, components will remain platform-agnostic.
Since [OpenAI](https://openai.com/)'s chat completion API, also available through as
[Microsoft Azure OpenAI service](https://azure.microsoft.com/en-us/products/cognitive-services/openai-service/)
is already commercially available at scale, competitively priced and arguably the most powerful system at the moment,
we currently use it as backend. If there is demand, we intend to provide alternative backends using other systems
as they become commercially available,
especially as those might be preferred by European customers because of stricter privacy and
data security rules. It's likely that in time there will also be LLM that can be run on premise.

## Functionality

For a quick view of the Composum Pages variant, please compare the 
[Composum blog entry](https://www.composum.com/home/blog/pages/composumAI.html) about it and the more detailed
[documentation](https://www.composum.com/home/pages/editing/Composum-AI.html); for the AEM variant there is also a
[blog entry](https://www.composum.com/home/blog/AEM/composumAI-AEM.html)
presenting the functionality.

There are the following dialogs that help with editing in Composum Pages. For the AEM version we omit the translation
dialog (as AEM has it's own frequently used mechanisms) and the page
category dialog, as AEM tags work quite differently.

- a translation dialog that can be opened on any edited textfield in multilingual websites and suggests translations
  of the text of a component into other languages the text of page descriptions and summaries, keywords, navigation
  title
- a multi purpose content creation dialog that can support you by creating text for any edited text fields. The text
  can be created either from a user supplied prompt, or by applying a (user supplied or predefined) ChatGPT prompt
  to the existing text of the page / component. That way, one could create summaries of the page, or extracts wrt. a
  topic, overviews, orjust request suggestions to reformulate or expand user supplied text fragments, ... Your
  imagination is the limit.
- an side panel AI that you can chat with and use to analyze the text content of the current page and get suggestions.
- a page category dialog with suggestions for page categories (that are used as SEO keywords)

## Try it out!

The easiest way to try it is the [Composum Cloud](https://cloud.composum.com) where Composum-AI is deployed. You can 
get a free account there, create a site and test it. 
Second there is the Composum Sling starter (a JAR you can execute locally) or the docker image
[composum/featurelauncher-composum](https://hub.docker.com/r/composum/featurelauncher-composum)
available through the
[Composum Launcher](https://github.com/ist-dresden/composum-launch) project - see there for description.
You will need an [OpenAI API key](https://platform.openai.com/account/api-keys) secret key to run it, and configure
that in the [Felix Console configuration tab](http://localhost:8080/system/console/configMgr) at "GPT Chat
Completion Service".

For AEM, there is a
[package on maven central](https://central.sonatype.com/artifact/com.composum.ai.aem/composum-ai.all) to deploy. You 
will need to configure an OpenAI API key as well.

Please compare the release notes of the 
[latest release](https://github.com/ist-dresden/composum-AI/releases/) for more instructions.

## Further ideas in investigation

- possibly support of the user when searching the site using natural language
- possibly support of the user by answering questions about the site content

## Development with ChatGPT and Github Copilot

Part of the intention of the project is to evaluate the use of AI services, in particular ChatGPT and the Github
Copilot IntelliJ plugin, for speeding up development. Some outcomes of that:

- The [feature creation process](featurespecs/FeatureCreationProcess.md) massively improves the quality and speed of
  specifying a feature and thinking it through. See the [feature specification directory](featurespecs/) for
  examples of that.
- Speeding up the development is ongoing, but Github Copilot and ChatGPT certainly
[speed up a lot of things](http://www.stoerr.net/blog/2023-05-25-developmentWithChatGPTAndCopilot.html).

## More project documentation

[Architecture](./Architecture.md) contains a description of the chosen project architecture and records
architectural decisions.

[Project structure](./ProjectStructure.md) describes the module structure of the project.

[ChatGPT API Analysis](./ChatGPTAPI.md) contains a discussion of the ChatGPT API wrt. our project.

[Next steps](./NextSteps.md) contains a sketch of the next steps to be taken.

## Thanks

Thanks to [OPAX](https://github.com/jaketracey/opax) for many ideas for prompts.

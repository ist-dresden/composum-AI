# Composum AI

> Fast and flexible,<br>
Composum empowers you,<br>
Content blooms with ease.<br>
-- ChatGPT

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
creation, review, summarizing, chat, you name it.

Composum AI provides AI services for [Composum Pages](https://www.composum.com/home.html). There is also a version for 
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html),
though yet somewhat experimental. Both share quite some commonality through being based on the
[Apache Sling](https://sling.apache.org/) platform.
Where possible and appropriate, components will remain platform-agnostic.
Since [OpenAI](https://openai.com/)'s chat completion API, also available through as
[Microsoft Azure OpenAI service](https://azure.microsoft.com/en-us/products/cognitive-services/openai-service/)
is already commercially available at scale, competitively priced and arguably the most powerful system at the moment,
we currently use it as backend. As other systems become commercially available, we intend to provide alternative 
backends using these, too, especially as those might be preferred by European customers because of stricter privacy and 
data security rules. It's likely that in time there will also be LLM that can be run on premise.

## Current status

There are the following dialogs that help with editing in Composum Pages:

- a translation dialog that can be opened on any edited textfield in multilingual websites and suggests translations
  of the text of a component into other languages the text of page descriptions and summaries, keywords, navigation title
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

# Composum AI - AI-powered content creation integrated in your CMS

> Fast and flexible, \
> Composum empowers you, \
> Content blooms with ease. \
> -- ChatGPT

We provide several content creation assistants that are tightly integrated into
[Composum Pages](https://www.composum.com/) or
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html)
putting the power of AI supported content creation at your fingertips.

[Click for a 2.5min demonstration video](https://github.com/ist-dresden/composum-AI/assets/999184/18595f2a-e0b5-49f3-bc4c-65d6a8bc93f6)

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

## Functionality Overview

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

## Thanks

Thanks to [OPAX](https://github.com/jaketracey/opax) for ideas for prompts.
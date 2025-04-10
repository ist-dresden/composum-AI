---
title: Composum AI – Enhancing Content Management with AI
description: Artificial intelligence for the editor in Composum Pages CMS and Adobe AEM : content creation and analysis, translation, suggestions, ...
---

<!-- https://github.blog/2008-12-19-github-ribbons/ -->
<a href="https://github.com/ist-dresden/composum-AI" style="position: absolute; top: 0px; right: 0px;">
  <img decoding="async" width="149" height="149" src="https://github.blog/wp-content/uploads/2008/12/forkme_right_gray_6d6d6d.png?resize=149%2C149" class="attachment-full size-full" alt="Fork me on GitHub" loading="lazy" data-recalc-dims="1"></img>
</a>

# Composum AI - AI-powered content creation integrated into Adobe Experience Manager / Composum Pages

> Fast and flexible, \
> Composum empowers you, \
> Content blooms with ease. \
> -- ChatGPT

We provide several content creation assistants that are tightly integrated into
[Composum Pages](https://www.composum.com/) or
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html)
putting the power of AI supported content creation at your fingertips.

Click for a 2.5min demonstration video of
[Composum AI for Composum Pages](image/ai/video/QuickDemo.mp4) or
[Composum AI for Adobe Experience Manager (AEM)](image/ai/video/ComposumAIforAEMQuickDemo.mp4), or watch these on
Youtube:

<div style="display: flex; flex-wrap: wrap; justify-content: space-between; margin-top: 2em;">
  <div style="flex: 0 0; margin-bottom: 2em; text-align: center;">
    <iframe src="https://www.youtube.com/embed/96gv-F4zX_o?si=eOXepyR2uP-tPjXp" 
        title="Living Slideshow for the Composum AI for Adobe AEM" frameborder="0" width="560" height="315" 
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
        allowfullscreen="allowfullscreen"></iframe>
    <p>Living Slideshow for the Composum AI for Adobe AEM</p>
  </div>

  <div style="flex: 0 0; margin-bottom: 2em; text-align: center;">
    <iframe src="https://www.youtube.com/embed/lSdKlwIDPkE?si=IRPONQn1zBk_0CZ0" 
          title="Living Slideshow for the Composum AI for the Composum Pages CMS" 
          frameborder="0" width="560" height="315" 
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowfullscreen="allowfullscreen"></iframe>
    <p>Living Slideshow for the Composum AI for the Composum Pages CMS</p>
  </div>
</div>

## Basic idea

With the advent of large language models (LLM) like [ChatGPT](https://openai.com/blog/chatgpt),
content creation in content management systems like [Composum Pages](https://www.composum.com/home.html) or the
[Adobe Experience Manager](https://business.adobe.com/products/experience-manager/adobe-experience-manager.html)
can become considerably easier with the new support by AI systems.
While [OpenAI](https://openai.com/)'s ChatGPT was and is probably the most noteable forerunner,
there is quite number of commercial systems like
[Anthropic Claude](https://www.anthropic.com/claude) (UK),
[Google's Bard](https://bard.google.com/),
[Aleph's Luminous](https://www.aleph-alpha.com/luminous) (Germany) in development, to name only a few, and 
increasingly also models you can run on local machines. 
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
we currently use it as backend. Since various open source LLM
[can be run with a similar interface](https://medium.com/thedeephub/50-open-source-options-for-running-llms-locally-db1ec6f5a54f)
it's also possible to use these, e.g. with 
[LM Studio](https://lmstudio.ai/) or [ollama](https://github.com/ollama/ollama). If you need alternative backends, please
[contact us](https://www.composum.com/home/contact.html)
- normally that will need only small changes.
It's recommendable, though, to use a model with >= GPT 3.5 performance.

## Functionality Overview

For a quick view of the Composum Pages variant, please compare the
[Composum blog entry](https://www.composum.com/home/blog/pages/composumAI.html) about it and the more detailed
[usage documentation](composum-variant/usage.md); for the AEM variant there is also a
[blog entry](https://www.composum.com/home/blog/AEM/composumAI-AEM.html)
presenting the functionality, and the [usage documentation](aem-variant/usage.md) on this site.

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
- a side panel AI that you can chat with and use to analyze the text content of the current page and get suggestions.
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
Completion Service". Please consult the [installation instructions](composum-variant/installation.md) for more details.

For AEM, there is a
[package on maven central](https://central.sonatype.com/artifact/com.composum.ai.aem/composum-ai.all) to deploy. You
will need to configure an OpenAI API key as well. The [installation instructions]
(aem-variant/installation.md) contain more details.

Please also compare the release notes of the
[latest release](https://github.com/ist-dresden/composum-AI/releases/) for more instructions.

## Development with ChatGPT and Github Copilot

Part of the intention of the project is to evaluate the use of AI services, in particular ChatGPT and the Github
Copilot IntelliJ plugin, for speeding up development. Some outcomes of that:

- The [feature creation process](featurespecs/FeatureCreationProcess.md) massively improves the quality and speed of
  specifying a feature and thinking it through. See the [feature specification directory](featurespecs/) for
  examples of that.
- Speeding up the development is ongoing, but Github Copilot and ChatGPT certainly
[speed up a lot of things](http://www.stoerr.net/blog/2023-05-25-developmentWithChatGPTAndCopilot.html).

## License

Composum AI is open source and uses the MIT license. So you have many freedoms to use it. But please be sure to sent us
some feedback!

## Thanks

Thanks to [OPAX](https://github.com/jaketracey/opax) for many ideas for prompts.

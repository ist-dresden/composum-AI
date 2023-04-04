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

## Planned services

There are a number of ways ChatGPT's services could be valuable in a CMS. Those include the following:

- for multilingual websites: suggestions for the translation of the text of a component into several languages
- suggestions for the text of page descriptions and summaries, keywords, navigation title
- entry of ChatGPT generated text content into text fields / text areas , either from a user supplied ChatGPT prompt,
  or a (user supplied or predefined) ChatGPT prompt applied to the existing text of the page or component or text
  selected on the page. That way, one could create summaries of the page, or extracts wrt. a topic, overviews, or
  just request suggestions to reformulate or expand user supplied text fragments
- possibly support of the user when searching the site using natural language
- possibly support of the user by answering questions about the site content

We will start with simple services and build on that.

## More project documentation

[Next steps](./NextSteps.md) contains a sketch of the next steps to be taken.

[Architecture](./Architecture.md) contains a description of the chosen project architecture and records 
architectural decisions.

[Project structure](./ProjectStructure.md) describes the module structure of the project.

[ChatGPT API Analysis](./ChatGPTAPI.md) contains a discussion of the ChatGPT API wrt. our project.

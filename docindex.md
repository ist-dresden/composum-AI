# Documentation Index

This file serves as an index for all .md files in the project. It provides a brief description of what information can be found in each file.

Generated with ChatGPT plugin [DevelopersChatGPTToolBench](https://github.com/stoerr/DevelopersChatGPTToolBench) and 
prompt:
    Create a file docindex.md that contains an index of all .md files in the project, as a helper for locating a documentation. For each .md file it should have an entry that tells what information is found there. (Not a summary - the file should help to find the right .md file). Index only markdown files which are named with extension .md, nothing else.
    Go step by step: first check what entries docindex.md already has, then list what .md files are there, read a .md file for which there is no entry yet and create it in docindex.md, until all files are indexed. 

## The index

- [Architecture.md](Architecture.md): This file documents the architecture of this application and records implementation decisions that were taken and their reasons.
- [BusinessPerspective.md](BusinessPerspective.md): This file discusses the Composum AI project from a business perspective, detailing how it enhances content management in Composum Pages by providing AI-assisted features such as translation, content creation, and SEO keyword suggestion.
- [ChatGPTAPI.md](ChatGPTAPI.md): This file provides an analysis of the ChatGPT API in relation to the needs of the project. It focuses on the ChatGPT chat API with gpt-3.5-turbo.
- [NextSteps.md](NextSteps.md): This file outlines the next steps in the implementation of the project, including the basic steps for implementing a translation service.
- [ProjectStructure.md](ProjectStructure.md): This file provides an overview of the planned project structure, including the backend services and the basic strategies for ChatGPT API access.
- [README.md](README.md): This file provides an overview of the Composum AI project, including the basic idea, current status, future ideas, and how to try it out. It also discusses the use of AI services, in particular ChatGPT and the Github Copilot IntelliJ plugin, for speeding up development.
- [Releasing.md](Releasing.md): This file contains internal remarks about preparing a release for the Composum AI project, including a checklist of steps to follow.
- [Videos.md](Videos.md): This file contains notes on what to show in the demo videos for the Composum AI project.
- [chatgpt.codeRules.md](chatgpt.codeRules.md): This file outlines the general rules for ChatGPT coding in this project, including guidelines on clean code conventions, best practices, and specific instructions for unit testing.
- [composum/ComposumIntegration.md](composum/ComposumIntegration.md): This file provides details about the Composum integration of the module, including the location of integration in the UI, the mechanism for embedding buttons into widget labels, and relevant files in Composum Pages.
- [composum/package/ComposumSpecificPrompts.md](composum/package/ComposumSpecificPrompts.md): This file contains prompts for ChatGPT for Composum specific things, including instructions for adding title attributes for the UI elements and rewriting labels.
- [featurespecs/0TranslationDialog.md](featurespecs/0TranslationDialog.md): This file provides a detailed feature specification of the Translation Dialog, including the background, basic feature idea, basic implementation decisions, user workflow, dialog elements, structure of the dialog, user interaction diagram, stati of the dialog, Composum implementation references, test cases, and possible extensions.
- [featurespecs/1ContentCreationDialog.md](featurespecs/1ContentCreationDialog.md): This file provides a comprehensive feature specification of the Content Creation Dialog. It includes the basic idea, implementation decisions, user workflow, dialog elements, structure of the dialog, user interaction diagram, saving state, implementation plan, test cases, and possible extensions.
- [featurespecs/2PageCategoryDialog.md](featurespecs/2PageCategoryDialog.md): This file provides a detailed feature specification for a dialog to support setting page categories in Composum Pages, utilizing the AI capabilities of ChatGPT to suggest categories.
- [featurespecs/3SidebarDialog.md](featurespecs/3SidebarDialog.md): This file provides a feature specification of the general sidebar AI Dialog in Composum Pages, detailing the integration of a new AI-assisted sidebar in the WYSIWYG editor for creating, editing, and arranging page components.
- [featurespecs/4SSEStreaming.md](featurespecs/4SSEStreaming.md): This file discusses the use of streaming responses to improve user experience in the Composum AI project, focusing on the use of the Server-Sent Events (SSE) streaming API due to the slow response time of the ChatGPT completion API.
- [featurespecs/ComposumIntegrationArchitecture.md](featurespecs/ComposumIntegrationArchitecture.md): This file provides an overview of the general architecture of the Composum integration in the Composum AI project, including details on naming conventions and servlets.
- [featurespecs/FeatureCreationProcess.md](featurespecs/FeatureCreationProcess.md): This file provides ideas and guidelines on how to create a feature with ChatGPT support, including an approach using the ChatGPT chat interface for feedback and support during the feature specification and implementation process.
- [featurespecs/FeatureIdeas.md](featurespecs/FeatureIdeas.md): This file is a brainstorming list of potential features that could be supported in the Composum AI project, although they are not currently planned for implementation.
- [featurespecs/SecurityConsiderations.md](featurespecs/SecurityConsiderations.md): This file discusses the security considerations that need to be taken into account across all features in the Composum AI project, including the potential problem of prompt injection.

# Next steps in the implementation

## Small bugs

- ??? "Bug, please report: no widget found for /content/ist/composum/home/pages/setup/jcr:
  content/main/row/column-0/section/codeblock/code"

## In evaluation

- Choose tags from existing tags, AEM like?
- Page assistant in preview: can also process selected text!
- Site specific or global templates for prompts.
- Global configuration for prompts: e.g. glossary, important terms, background information.
- Bug: translation in all properties component - fill german value and translate to english.
- Check Error handling. (e.g. connection error.)
- Unclear: content generation from text in non-default language?
- better mode of shortening texts if required text length is small.

## Possible improvements later

- possibly: edit text in translation, after all? That's better because only there you see the original.
- batch mode translation for a whole page / site for Composum, too
- Prompt registry where people can put interesting prompts and like them (social component)
- perhaps use moderation api to check for troublesome user messages and troublesome responses (?)

## Won't do

- (Extend content creation assistent with selection as input. (Not possible on page, but there.) -> could also
  replace copy the selection and put that into the explicit input. )
- Check where it should appear in Pages and where not - extend tag.
  - Autor, key Felder

# Archive

## Basic steps for implementing a translation service

- DONE: choose framework to access ChatGPT / library / code generation; implement basic access to ChatGPT chat API
- DONE: implement simple translation service
- DONE: implement strategies to deal with rate limiting: timed retry when we hit the rate limit of the ChatGPT API
- DONE: implement simple keyword creation service
- DONE: caching of requests to prevent unnecessary repetition
- (DONE) timed delay of access or even denial when the user hits a configurable request frequency or request number (the
  price per ChatGPT chat request is rather low, but we still have to prevent DOS attacks or worse.) TODO: make user
  specific bounds.
- DONE: implement use of translation service in Composum Pages as new dialog.

## Steps towards ChatGPT prompts wrt. page content

- DONE: integrate translation suggestions into Composum Pages
- DONE: implement markdown (ChatML) rendering for Composum Pages components to easily retrieve a textual
  representation of the page
- DONE: implement suggestions for page description and keywords (= categories) in pages
- DONE: implement ChatGPT dialog that can be called from basic text components, and gives the user the possibility to
  prompt ChatGPT

##

- DONE: titles
- DONE: Help page for dialogs
- DONE: Drag dialogs
- DONE: use streaming responses https://github.com/openai/openai-cookbook/blob/main/examples/How_to_stream_completions.
  ipynb to improve user experience.
  https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format
- DONE: Page assistant: writing style
- DONE: use library to count the tokens -> JTokkit
- DONE: save scroll position, scroll the chat field to the top.
- DONE Somehow implement streaming to make result more responsive.
- DONE: Update AEM dialog with history
- DONE: AEM 6.5 version
- DONE: API key in site configuration? Tenant configuration?
- DONE: AEM announcement and installation and usage description
- DONE: Update Pages content creation dialog with separate content field like AEM

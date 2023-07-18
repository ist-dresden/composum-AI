# Next steps in the implementation

- Check where it should appear and where not - extend tag.
    - Autor, key Felder
- Base text as explicit field filled on ddl choice
    - Base text ("data")
    - Prompt ("instructions")
- API key in site configuration? Tenant configuration?
- (Explain: why isn't it there in edit styles)
  
- ??? Konfiguration f. Seiten?
- ??? Input prompt nicht ersetzen? Predefined
- Bug: for new component the history key is wrong in the create dialog

## Small bugs

- Doppelklick darf Dialog nicht mehrfach öffnen!
- Stacked Modals: scroll des untersten?
- Bug, please report: no widget found for /content/ist/composum/home/pages/setup/jcr:
  content/main/row/column-0/section/codeblock/code
- Reset history button for create dialog
- Reset in create dialog should clear response, too

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
- batch mode translation for a whole page / site
- Prompt registry where people can put interesting prompts and like them (social component)
- perhaps use moderation api to check for troublesome user messages and troublesome responses
- use Composum platform caching service, possibly with an abstraction.

## Won't do

- (Extend content creation assistent with selection as input. (Not possible on page, but there.) -> could also
  replace the selection / insert created text at point. Alternative: explicit input. Isn't really necessary, since
  user can just incorporate that into the prompt, or put it into the content suggestion field and iterate over that.)

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

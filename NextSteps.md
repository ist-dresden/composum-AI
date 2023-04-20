# Next steps in the implementation

## Basic steps for implementing a translation service

- DONE: choose framework to access ChatGPT / library / code generation; implement basic access to ChatGPT chat API
- DONE: implement simple translation service
- DONE: implement strategies to deal with rate limiting: timed retry when we hit the rate limit of the ChatGPT API
- DONE: implement simple keyword creation service
- implement use of translation service in Composum Pages
- caching of requests to prevent unnecessary repetition
- timed delay of access or even denial when the user hits a configurable request frequency or request number (the
  price per ChatGPT chat request is rather low, but we still have to prevent DOS attacks or worse.)

## Steps towards ChatGPT prompts wrt. page content

- integrate translation suggestions into Composum Pages
- implement markdown (ChatML) rendering for Composum Pages components to easily retrieve a textual representation of
  the page
- implement suggestions for page description and keywords (= categories) in pages
- implement ChatGPT dialog that can be called from basic text components, and gives the user the possibility to
  prompt ChatGPT

### Structure
- markdown rendering: check component structure. -> necessary for keywords etc.

## Caching

It seems sensible to cache some kinds of requests to ChatGPT, since, first, they cost a little money, and second,
they are slow. At least translation requests might easily get repeated, and some requests might at least involuntarily
be repeated by the user. However, that is dependent on whether I'm using AEM or Composum. So we'd want to define a
service interface in the backend, and provide an implementation in the Composum implementation (based on the 
existing platform cache) / AEM implementation. 
Annoyingly this seems to require two additional bundles with basically one class each, to avoid circular dependencies: 
one defining the cache interface in backend, one to implement it in Composum / AEM.
Possibly idea that would provide some (though very little) additional benefit: separate backend into an api and impl 
bundle? Or use a small translation cache in a HashMap anyway, since that seems the only service where repeated 
requests are likely?

## Possible improvements later

- use streaming responses https://github.com/openai/openai-cookbook/blob/main/examples/How_to_stream_completions.
  ipynb to improve user
  experience. https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format
- perhaps use moderation api to check for troublesome user messages and troublesome responses

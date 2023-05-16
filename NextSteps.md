# Next steps in the implementation

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

## Possible improvements later

- use streaming responses https://github.com/openai/openai-cookbook/blob/main/examples/How_to_stream_completions.
  ipynb to improve user
  experience. https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format
- perhaps use moderation api to check for troublesome user messages and troublesome responses
- use Composum platform caching service, possibly with an abstraction.

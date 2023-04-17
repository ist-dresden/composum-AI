# Next steps in the implementation

Just a rough sketch of the first steps for now.

## Basic steps for implementing a translation service

- choose framework to access ChatGPT / library / code generation; implement basic access to ChatGPT chat API
- implement simple translation service
- implement strategies to deal with rate limiting:
    - timed retry when we hit the rate limit of the ChatGPT API
    - timed delay of access or even denial when the user hits a configurable request frequency or request number (the
      price per ChatGPT chat request is rather low, but we still have to prevent DOS attacks or worse.)
    - caching of requests to prevent unnecessary repetition
- implement use of translation service in Composum Pages

## Steps towards ChatGPT prompts wrt. page content

- implement markdown (ChatML) rendering for Composum Pages components to easily retrieve a textual representation of
  the page
- implement suggestions for page description and keywords (= categories) in pages
- implement ChatGPT dialog that can be called from basic text components, and gives the user the possibility to
  prompt ChatGPT

## Possible improvements later

- use streaming responses https://github.com/openai/openai-cookbook/blob/main/examples/How_to_stream_completions.
  ipynb to improve user experience. https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format
- perhaps use moderation api to check for troublesome user messages and troublesome responses

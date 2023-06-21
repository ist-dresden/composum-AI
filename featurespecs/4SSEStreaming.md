# Use streaming responses to improve user experience

https://github.com/ist-dresden/composum-AI/issues/5

## Basic Idea

Unfortunately, the ChatGPT completion API is quite slow, so the user has to wait seconds to even tens of seconds for
the response to appear. Therefore it is a must that we use the streaming API with Server-Sent Events (SSE)
https://github.com/openai/openai-cookbook/blob/main/examples/How_to_stream_completions.ipynb
https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#event_stream_format
so that the user can already see parts of the response while it's generating.

## General requirements

- SSE are supported by most browsers, so we should be good, but it's better to leave a fallback to normal ajax.
- The EventSource supports only GET requests, but for most requests we have an amount of data that is way too large
  for GET requests. So it's necessary to do a POST request, which returns a (cryptographically secure!) ID that is
  then used with an EventSource GET request to receive the streaming response.
- We want to be able to abort the streaming from the Browser (e.g. if the user hits a "Generate" button again).

## Implementation remarks

The HttpClient contained in the JDK has an implementation using a Flow.Subscriber to receive lines. It's natural to
use that mechanism also for the communication of the composum module with base module.

GPTChatCompletionService:
void streamingChatCompletion(@Nonnull GPTChatRequest request, @Nonnull GPTCompletionCallback callback) throws GPTException;

The callback implements Flow.Subscriber<String> . In the composum layer we save that in the session with an 
cryptographically secure ID and return that ID to the browser, which triggers another call using an EventSource to 
receive the parts.

## Things to consider

2. **Security of the Temporary ID:** When the POST request is made and a temporary ID is returned, what measures will we
   take to ensure that this ID cannot be misused or intercepted? Also, how long will the ID be valid for?

5. **Abort Mechanism:** How can we implement an abort mechanism that is both user-friendly and resource-efficient? We
   need to make sure that aborting a response generation does not leave any "zombie" processes on the server.

7. **Error Handling:** What happens if an error occurs during the streaming process? We need a robust error handling
   mechanism both on the client and server side.

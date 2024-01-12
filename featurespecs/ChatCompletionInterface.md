# Description of the ChatGPT Chat Completion Interface for code generation

## Links

https://platform.openai.com/docs/api-reference/chat/create
https://platform.openai.com/docs/guides/vision
We currently ignore tools and logprobs.

## Basic implementation decisions

We use GSON for JSON serialization and deserialization. We use the `@SerializedName` annotation to map the JSON
attributes to the Java attributes if the attribute name has to be different due to Java naming conventions.

Package `com.composum.ai.backend.base.service.chat.impl.chatmodel`
(folder `backend/base/src/main/java/com/composum/ai/backend/base/service/chat/impl/chatmodel`)
contains the generated Java classes for the JSON objects. Use the JavaBean conventions.
The class names should be prefixed with ChatCompletion to avoid name clashes.
Enumerations should be used for the fixed values and these should be inner classes in the class where they are used.

## Request

Attribute `role` has the fixed values `user`, `assistant` and `system` and should be an enum in the generated code.
Attribute `type` can have the fixed values `text` and `image_url` and should be an enum in the generated code.

```json
{
  "model": "gpt-3.5-turbo",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant."
    },
    {
      "role": "assistant",
      "content": "Hello!"
    },
    {
      "role": "user",
      "content": [
        {
          "type": "text",
          "text": "What’s in this image?"
        },
        {
          "type": "image_url",
          "image_url": {
            "url": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/Gfp-wisconsin-madison-the-nature-boardwalk.jpg/2560px-Gfp-wisconsin-madison-the-nature-boardwalk.jpg"
          }
        }
      ]
    }
  ],
  "max_tokens": 300,
  "stream": true
}
```

## Response

Attribute `role` has the fixed values `user`, `assistant` and `system` and should be an enum in the generated code.
Attribute `logprobs` should be ignored in the generated code.
Attribute `finish_reason` has the fixed values `stop`, `length`, `content_filter` and should be an enum in the generated
code.

The normal response and the streaming response should be mapped to the same Java class.

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "gpt-3.5-turbo-0613",
  "system_fingerprint": "fp_44709d6fcb",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "\n\nHello there, how may I assist you today?"
      },
      "logprobs": null,
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 9,
    "completion_tokens": 12,
    "total_tokens": 21
  }
}
```

## Streaming Response

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1694268190,
  "model": "gpt-3.5-turbo-0613",
  "system_fingerprint": "fp_44709d6fcb",
  "choices": [
    {
      "index": 0,
      "delta": {
        "role": "assistant",
        "content": ""
      },
      "logprobs": null,
      "finish_reason": null
    }
  ]
}
```

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1694268190,
  "model": "gpt-3.5-turbo-0613",
  "system_fingerprint": "fp_44709d6fcb",
  "choices": [
    {
      "index": 0,
      "delta": {
        "content": "Hello"
      },
      "logprobs": null,
      "finish_reason": null
    }
  ]
}
```

....

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1694268190,
  "model": "gpt-3.5-turbo-0613",
  "system_fingerprint": "fp_44709d6fcb",
  "choices": [
    {
      "index": 0,
      "delta": {
        "content": "?"
      },
      "logprobs": null,
      "finish_reason": null
    }
  ]
}
```

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion.chunk",
  "created": 1694268190,
  "model": "gpt-3.5-turbo-0613",
  "system_fingerprint": "fp_44709d6fcb",
  "choices": [
    {
      "index": 0,
      "delta": {},
      "logprobs": null,
      "finish_reason": "stop"
    }
  ]
}
```

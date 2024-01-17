# Add ChatGPT image recognition as additional input

## Links

https://platform.openai.com/docs/guides/vision
https://platform.openai.com/docs/api-reference/chat/object
java ImageIO

    "messages": [ { "role": "user",
          "content": [
            {
              "type": "text",
              "text": "What’s in this image?"
            },
            {
              "type": "image_url",
              "image_url": {
                "url": f"data:image/jpeg;base64,{base64_image}"
              }
            }
          ]
        }
    ]

## Implementation remarks

We extend the content creation dialog with vision features: it is possible to select an image as source and use
the model gpt4-vision-preview to process it. (That is currently in beta and has some limitations.)

Vision has to be optional, since it needs the a bit more pricey gpt4 models. If it is not switched on, the image
selection in source models needs to be switched off, and the "describe image" prompt should not be there.

In GPTChatCompletionServiceImpl the default model is configured, and we also need to configure the vision model there.
If that is not present, vision has to be off.

In the case of Composum, the selectors are read in the class
com.composum.ai.composum.bundle.model.CreateDialogModel with the method getContentSelectors and the prompts with
getPredefinedPrompts.

In the case of AEM the content selectors are read from datasource composum-ai/servlets/contentcreationselectors
(ContentCreationSelectorsServlet)
and the predefined prompts are read from datasource /conf/composum-ai/settings/dialogs/contentcreation/predefinedprompts

Since there is currently only one vision related prompt, it's difficult to filter it out and the predefined prompts
need reworking for language dependence, anyway, we do not filter out that prompt.

The simplest way to implement this is to provide isVisionEnabled inthe GPTChatCompletionService , being true if a model
is set.

The GPTChatMessage was extended with imageUrl as additional attribute to provide for images.

## Test resources

Composum: teasers or http://localhost:9090/bin/pages.html/content/ist/composum/home/blog/nodes/restrictions

AEM: teasers and experience fragments when images are present.
http://localhost:4502/editor.html/content/experience-fragments/wknd/us/en/adventures/adventures-2021/master.html

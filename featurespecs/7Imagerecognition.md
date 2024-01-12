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

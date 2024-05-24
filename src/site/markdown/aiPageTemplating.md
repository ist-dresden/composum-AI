# AI Page Templating : AI supported page creation in one step (experimental)

While the content creation dialog can support you for creating individual texts in a page, there might be a good use
case for AI when you have to create many pages of the same type. For example, a product page could consist of a
title, an attention grabber, an introduction, a list of features, a call to action, or the AIDA framework. With our
AI Page templating you can create a template page that contains prompts (instructions) to create suggestions for all of 
these texts in the page, and when creating a new page from that template you supply the AI engine with the 
background information as source to generate the content with texts based on these prompts.

The Composum AI contains an experimental feature for this: it works, but further improvements and changes are likely.
Please try it out; you are invited to use it! Please give us [feedback](https://www.composum.com/home/contact.html) 
on what you think about it and what you would like to see improved!

## How a template page looks like

Any normal page can be used as template page. However, instead of a normal text you can also put prompts into every 
text field by starting the text with the marker `PROMPTFIELD: ` and then the instructions. For example, you could
have a text field with the content `PROMPTFIELD: single sentence invitation to check out the product`. When 
filling in the page texts, the AI will later replace the prompt with the generated text, based on the supplied 
source information. There are other markers:

| Marker             | Description                                                                                                                                          |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `PROMPTFIELD: `    | A field that is a prompt. Fields that do not start with this marker will not be touched.                                                             |
| `PROMPTFIELD#ID: ` | A prompt field that has an ID so that it can be referred to by other prompt fields.                                                                  |
| `SOURCEURL(url)`   | A URL source that is added as background information when generating the texts for the whole page. Only considered when it occurs in a prompt field. |
| `PAGEPROMPT: `     | A prompt that applies to the whole page - such as intended style and tone. (Only considered when it occurs in a prompt field.)                       |

## How to create a page from a template

<div style="float: right; margin-left: 20px;">
   <a href="image/ai/prototypes/AIPageTemplatingMiniApp.png">
    <img src="image/ai/prototypes/AIPageTemplatingMiniApp.png" alt="The AI templating mini app" width="400" />
  </a>
</div>

You have to copy the template page with the normal editor to the desired page location. Then you can trigger the 
Composum AI to collect the prompts from the page, supply it with the source information and have the language model
generate the texts.
To trigger this creation process there is a mini application 'AI Templating' linked at the protoype lists:

- Composum: [/libs/composum/pages/options/ai/prototype.html](http://localhost:9090/libs/composum/pages/options/ai/prototype.html)
- AEM: [/apps/composum-ai/prototype.html](http://localhost:4502/apps/composum-ai/prototype.html)

You have to enter the path to the page and either enter an URL that has text information that gives all needed 
background information for the texts, or you can paste the text directly into a "Background information" field.
The "Replace prompts" button will then replace the prompts in the page with the generated texts. (That can take a 
minute or two.) The "Reset to Prompts" button will reset the page to the original prompts, which are saved in the 
page itself. Thus, you can experiment and improve the instructions you give to the AI.  

## Requirements

The templating engine needs a model with good reasoning capabilities. OpenAI's gpt-3.5-turbo often fails to follow 
the instructions, so a model with better capabilities, like gpt-4o, is needed. This can be configured as the "High 
intelligence model" in the Composum AI configuration - see the configuration documentation for the 
[AEM variant(aem-variant/configuration.md) or [Composum Pages variant](composum-variant/configuration.md).
Caution: the OpenAI API key has to have access to GPT-4 models.

If you use URLs to supply the AI with background information, you have to add appropriate URL whitelisting in the
`Composum AI Approximate Markdown Service Configuration` since accessing URLs is disabled by default for security
reasons.

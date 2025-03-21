# Autotranslate Model Comparison Mini-Application

## Idea

The page should demonstrate translation of a text into another language using all available models - compare
featurespecs/12ManyLLMBackends.md for more information.

It allows input of a text, selection of the models to use for translation and the target language - either by
specifying the jcr:language value or a page path from which the site language can be determined, or a textual
description of the language.

The translation is done with com.composum.ai.backend.base.service.chat.GPTTranslationService.streamingSingleTranslation ,
the available models are retrievable with
com.composum.ai.backend.base.service.chat.GPTBackendsService.getAllModels() .

## Page structure

The page should be as follows:

- Title
- Explanation what the page does
- checkboxes for all available models. They should be in a inline layout so that space is saved.
- buttons to select all models or clear all selections
- textarea to input the text to translate
- translate into all selected languages button. That triggers a POST to the page itself.
- table with the translation results: headline with model name and timing in milliseconds, and then the translated 
  text. The table is only present when the page is called with parameters for models and text to translate.

The page width should be fully used.

## Architecture

- use self-typed resource aem/ui.apps/src/main/content/jcr_root/apps/composum-ai/components/<feature>
- Use HTL
- Place the backend logic in a Sling Model with `@Model(adaptables = SlingHttpServletRequest.class)` at
  aem/core/src/main/java/com/composum/ai/aem/core/impl/autotranslate/AutotranslateModelCompareModel.java
- Use bootstrap from CDN with a nice professional looking MacOS silvery like UI
- Place javascript and css in the same directory as the HTL file, access e.g. as
  <link rel="stylesheet" href="${resource.path}/autotranslatemodelcompare.css">
- If appropriate store manually entered state in localStorage for resilience across refreshes. Keys should start 
  with `composumAI-autotranslatemodelcompare-`.
- On POST retrieve the /libs/granite/csrf/token.json token and set it into a hidden :cq_csrf_token field.

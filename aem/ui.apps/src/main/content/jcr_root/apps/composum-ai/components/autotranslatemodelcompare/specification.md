# Autotranslate Model Comparison Mini-Application

Show all models, retrievable with GPTBackendsService.getAllModels() 

## Architecture

- use self-typed resource aem/ui.apps/src/main/content/jcr_root/apps/composum-ai/components/<feature>
- Use HTL
- Place the backend logic in a Sling Model with `@Model(adaptables = SlingHttpServletRequest.class)` at 
  aem/core/src/main/java/com/composum/ai/aem/core/impl/autotranslate/<feature>Model.java
- Use bootstrap from CDN with a nice professional looking MacOS silvery like UI
- Place javascript and css in the same directory as the HTL file, access e.g. as 
  <link rel="stylesheet" href="${resource.path}/<feature>.css">
- If appropriate store manually entered state in localStorage for resilience across refreshes.


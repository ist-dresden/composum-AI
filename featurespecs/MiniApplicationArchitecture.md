# Architecture for mini applications

There are some small applications like
aem/ui.apps/src/main/content/jcr_root/apps/composum-ai/components/autotranslatemerge/
and
aem/ui.apps/src/main/content/jcr_root/apps/composum-ai/components/autotranslate/
that consist of one or a few pages and are not integrated into the AEM editor or other AEM components.
For these the following structure is used:

- use self-typed resource aem/ui.apps/src/main/content/jcr_root/apps/composum-ai/components/<feature>
- Use HTL
- Place the backend logic in a Sling Model with `@Model(adaptables = SlingHttpServletRequest.class)` at 
  aem/core/src/main/java/com/composum/ai/aem/core/impl/autotranslate/<feature>Model.java
- Place javascript and css in the same directory as the HTL file, access e.g. as 
  <link rel="stylesheet" href="${resource.path}/<feature>.css">
- If appropriate store manually entered state in localStorage for resilience across refreshes.
- Use Coral UI 3, e.g. with
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <sly data-sly-use.clientLib="/libs/granite/sightly/templates/clientlib.html"></sly>
    <sly data-sly-call="${clientLib.css @ categories='cq.authoring.editor.core'}"></sly>
    <sly data-sly-call="${clientLib.js @ categories='coralui3,granite.ui.foundation.content'}"></sly>

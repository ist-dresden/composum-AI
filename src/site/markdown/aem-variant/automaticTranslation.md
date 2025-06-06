# Rethinking automatic translation in AEM with LLMs

> In AEM's embrace,\
> Composum AI weaves tongues,\
> Seamless, swift, it blends.\
> -- ChatGPT

## General idea

Translating a site is often a rather large effort. Since modern LLM can both speak many languages and integrate
advanced reasoning capabilities, they can be a big help in translating a site - possibly to the point that
translating a site can amount to an automatic process where the editor only has to check the results and only
occasionally make minor changes.

We integrated a implementation into the Composum AI that can translate individual pages or entire sites
quickly.
It is very promising: a test translating the [WKND site](https://wknd.site/us/en.html) from English into German took
only about 10 minutes (with GPT-3.5, though),
and the translation showed little need for manual corrections. Translating into other languages like Spanish, Japanese,
Chinese, Korean seem to work nicely as well, as far as we can judge by translating the text back with
[Deepl Translator](https://www.deepl.com/translator) .

Also, we implement a novel process of integrating the translation transparently into the rollout process of live
copies, which allows for very easy updating of the translated sites whenever changes are made in the primary language
site.

It's quick and easy to try that on your own site - install it locally or on one of your test systems and check out
the result!
Please let us know what you think and how it works for you and contact us on any trouble!

<div style="float: right;  margin-top: 2em;  margin-bottom: 2em; padding-left: 2em; padding-right: 2em;">
<a href="../image/ai/video/AEMAutoTranslation.mp4" target="_blank">
<figure>
<img src="../image/ai/video/AEMAutoTranslatePoster.png" alt="Composum AI for AEM automatic translation Quick Demo" 
width="500px"/><br/>
<figcaption>
Quick demonstration of automatic translation of an AEM site with Composum AI
</figcaption>
</figure>
</a>
</div>

## Advantages of our approach using of LLM for translation

While being employable for providing quick and inexpensive machine translation, LLMs like ChatGPT offer advanced
reasoning capabilities that can improve the quality of the translation:

- Unlike most existing machine translation solutions for AEM we collect all the texts of one page, experience fragment
  or content fragment and present them together in one
  translation request to an LLM (currently ChatGPT). Thus, it can take the context of the text into account and
  translate it accordingly.
- By default, the LLM is instructed to be close to the original text, preserving the original style, tone, sentiment,
  and formatting. But:
- Additional instructions can be given to influence the writing style, finetuning the translation
  to the desired audience, and even to give background information about the site and the translation, if needed.
  That can range from minor changes like influencing how formal to address the reader to try major modifications like
  simplifying the language for a younger audience.
- While that's not currently implemented, we make preparations so that manual corrections of translations can be
  preserved when a retranslation of a modified text in the original language is done (see ["Differential
  re-translation"](#difftranslation) below).

Or course, the full power of the [Composum AI](usage.md) can then be used to further improve the translated texts.

## Why we are using live copies instead of language copies

Our translation process differs from the translation process Adobe suggests by one important point: the translated
site(s) are live copies of the primary language, not language copies. The reason for that is: while language copies
nicely allow translating a site initially, they are more difficult to handle when you are updating the site. But
live copies are just made for that: change a page and hit "rollout" and the changes are transferred,
including structure changes like new components or component deletions. So our
translation process integrates transparently into the rollout process: an additional rollout configuration
translates all texts into the language configured for the rollout target during the rollout action.

## Usage

There are currently three ways to trigger the translation (see below):

- an additional rollout configuration that transparently translates the rolled out page during rollout
- a workflow process that can trigger a rollout with these rollout configurations for translating a page or page
  tree from its blueprint
- for experimentation: a mini UI that can be used to translate pages / page trees and roll back the translations. That
  can be used to easily try out the results, but is not intended for production use.

Please notice that this only works of the page to translate is a live copy of the original.

## Assumed site structure and the translation process

Such an automatic translation process is especially useful for sites or pages where the different language versions are
very close to each other in content and structure and mostly the language is different. So, if we require that the
other languages are live copies of their primary language, changes can be easily rolled out to the other languages, and
the automatic translation takes care of the translation of the changed / new texts.

A site like this could look very similar do the
[MSM site structure](https://experienceleague.adobe.com/en/docs/experience-manager-65/content/sites/administering/introduction/msm)
Adobe suggests. As an example, the WKND site the structure would look like this (just mentioning languages `en` and
`de` and countries `us` and `de` for simplicity):

- `/content/wknd/language-masters/en` - the primary language master
- `/content/wknd/language-masters/de` is a __**live copy**__ of the primary language master, auto-translated. This
  is the difference to the normal usage of live copies, but it makes it clear where the source of a translation is and
  rolling
  out changes to the other languages easy.
- `/content/wknd/us/en` and `/content/de/de` are the resp. country sites, live copies of the corresponding language
  masters `/content/wknd/language-masters/en` and `/content/wknd/language-masters/de`. These
  are mostly unmodified copies of the language master plus some country-specific content.

Of course, for multilingual sites that do not have a country-specific structure this can be appropriately simplified.

A similar structure is used for experience fragments (`/content/experience-fragments/wknd/language-masters`) and
content fragments (`/content/dam/wknd`). These have to be translated first - an automatic translation `en` to `de`
process also replaces links to `en` fragments with links to `de` fragments, if they exist.

Whenever changes are introduced into the english language master, these can be rolled out to the other languages
since these are live copies. If the translation on rollout (see below) is activated, the changed texts will be
automatically translated. Otherwise, the translation can be triggered manually with the workflows or UI.

## Translation on Rollout of Live copies

The translation will be automatically done on rollout under the following conditions:

- The live copy has either to have a path like .../de/.. the language can be determined from, or is set to the desired
  language
- As rollout configuration there should be a configuration like the standard rollout config that updates the page,
  and then (only after that!) the "Composum AI Autotranslate" rollout configuration.

Then the pages will be transparently be translated on each rollout. Please notice that this changes the semantics of
the inheritance somewhat: if a component inherits from the blueprint that means that the texts of the component are
the AI translated texts of the blueprint, and if inheritance is cancelled the texts are manually changed.

Caution: When a re-translation has to be triggered e.g. because of a change in additional instructions, a rollout of a
single
page will do the job and trigger the rollout configuration. However, as of 07/24, when a rollout is triggered
for a page including all subpages in AEMaaCS, only those subpages are rolled out again whose blueprint is changed,
so this might not lead to the desired result. For this case a workflow process is provided (see below).

## Triggering translation with the workflows

The com.composum.ai.aem.core.impl.autotranslate.workflow.TriggerRolloutWorkflowProcess can be used in a workflow to
trigger a rollout to a given page and all subpages from their blueprints. That is, if the corresponding workflow is
triggered on a automatically translated page, it will check this page and all it's subpages whether they are live
copies and, if so, trigger a rollout from the blueprint to this page. It can be used in a workflow by adding a
workflow process step "Composum AI Rollout To Here" with arguments `{"recursive":false}` or `{"recursive":true}`,
depending on whether subpages should be rolled out.
Suggested title for the process step: "Composum AI Rollout Tree To Here Recursively" (if recursive=true) and
description: "Translates the page that is given as payload from it's blueprint.
The page has to be a live copy of the page it's translated from. Configured as recursive: rolls out tree of pages."

There is also a deprecated action com.composum.ai.aem.core.impl.autotranslate.workflow.AutoTranslateWorkflowProcess
that can be used to trigger a translation of the page from a workflow. It requires that the page is set up as a live
copy of the primary language. This is deprecated in favor of triggering a rollout, if needed by
TriggerRolloutWorkflowProcess.

Caveat: a workflow process step is called with a workflow service user, likely workflow-process-service . Thus, we
cannot easily make sure that the workflow initiator has the rights to perform this action and modify the page
through this rollout. This has to be made sure by other means in the workflow, e.g. by
[applying ACL for the specific workflow model to /var/workflow/models.](https://experienceleague.adobe.com/en/docs/experience-manager-65/content/sites/administering/operations/workflows-managing)

## Usage of the Testing UI

For quick experimentation and evaluation of additional instructions, we provide a very simple testing UI. The
entry point for the translation is the URL in the author server
[/apps/composum-ai/components/autotranslate/list.html](http://localhost:4502/apps/composum-ai/components/autotranslate/list.html)
It allows entering a path for a page or content / experience fragment or whole site and starting a translation process
for either that page only, or for that page and it's subpages.

Depending on the size of the site the translation can
take a couple of minutes - a list of triggered translations is provided, providing links to detail views with the
actual pages that were translated and the status / statistics of the translation. The detail view also provides a
button to cancel and undo the translation.

If enabled in OSGI, there is also an experimental UI at
[/apps/composum-ai/components/autotranslate-experiments/list.html](http://localhost:4502/apps/composum-ai/components/autotranslate-experiments/list.html)
that also provides a form to quickly roll back the translation for a subtree for quick experimentation.
This will also reenable inheritance of all properties and resources, and reset the
translated texts to the state before the automatic translation. That's not safe on production of course.

## Installation / Configuration

The preliminary implementation is integrated into the Composum AI version >= 0.8.0. The POC UI has to be enabled by OSGI
configuration. If you are newly installing the Composum AI:
it needs at least [configuration of the API key](configuration.md) for Composum AI in general; if you want to use
the POC UI you need to enable that in the "Composum AI Autotranslate Configuration"
(PID `com.composum.ai.aem.core.impl.autotranslate.AutoTranslateServiceImpl`) .

If the heuristics to determine the translated resources / properties are not sufficient for your site, you configure
exceptions in an OSGI configuration "Composum AI Autotranslate Configuration"
(see the [OSGI configuration reference](../autogenerated/osgiconfigurations.html#osgi.AutoTranslateConfig) ).

Additionally, there is a Sling contextaware configuration "Composum AI Automatic Translation Configuration"
that can be used to add additional instructions how to translate the pages in the subtree the configuration applies to
(compare [sling configuration reference](../autogenerated/slingcaconfigurations.html#slingca.AutoTranslateCaConfig) ).

### Translation rules

Since there can be different additional instructions for each page, there is the concept of a "translation rule"
that defines additional instructions with conditions when these are to be used. Such a rule has:

- an optional path pattern that has to match the path of the page
- an optional content pattern that has to appear in the content of the page
- the additional instructions to be added when both patterns match or no pattern is given.

A common use case would be to give specific translations for certain words. That'd be done by adding a rule with the
translated word / phrase as content pattern and "Translate XXX as YYY" as additional instruction.
However, if you have many rules of this type you might also use translation tables with glossaries for that. You can
upload a spreadsheet (CSV or Excel file) with the word to translate in one column and the suggested translation in
another column. The AI will then be given instructions accordingly for all words that actually appear in the page
that is being translated.

### Additional tools

For comparing the blueprint and the translated live copy there is a tool
`/apps/composum-ai/components/tool/comparetool.html/{pagepath}`
that finds the blueprint of the page with the given path and displays that blueprint and this page side by side,
with joined scrolling.

`/apps/composum-ai/components/autotranslatemerge/list.html/{pagepath}` starts a tool that displays the texts of
components where the inheritance was cancelled for manual modification, so that the translations are not automatically
updated, but for which there are changes in the source page that have been noted during rollout. 
You can review and edit translations for modified properties in the tool.
Only component properties where the inheritance has been cancelled and where
the blueprint was modified after cancelling inheritance or the last merging are displayed
- until they are saved and marked as merged.

`/apps/composum-ai/components/tool/bulkreplace.html` allows a bulk search and replace through a page tree, incl.
optionally creating page versions before replacement and automatic publishing. This omits internal properties that are
used for the AI translation (see it's specifications) but also replaces the text in those for the target language.

## Notes about the implementation

- To have a painless startup, the identification of properties that need translation is heuristic. Standard
  attributes like
  `jcr:title`,
  `jcr:description`, `text`, `title`, `alt`, `cq:panelTitle`, `shortDescription`, `actionText`, `accessibilityLabel`,
  `pretitle`, `displayPopupTitle`, `helpMessage` are translated if their values contain word characters. For other
  properties we check that they are not paths, contain whitespace and contain some word characters. That would exclude
  nonstandard properties containing single words from translation, and might lead to some false positives. For
  productive usage a configuration for positive and negative exceptions would likely be recommendable. That can
  currently be done with the "Composum AI Autotranslate Configuration".
- At the moment the translator can only be switched off or on, but cannot be restricted to certain users or content
  areas.

The [standard AEM translation process](https://experienceleague.adobe.com/docs/experience-manager-learn/sites/multi-site-management/updating-language-copy.html)
uses language copies. Besides the discussed advantages of live copies, integrating into that process is a bit
difficult. The machine translation process in translation connectors is heavily geared towards translating
individual texts independently from the translated page. Ultimately this amounts to implementing the method
[TranslationService.translateArray](https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/adobe/granite/translation/api/TranslationService.html#translateArray-java.lang.String:A-java.lang.String-java.lang.String-com.adobe.granite.translation.api.TranslationConstants.ContentType-java.lang.String-)
in the connector, which doesn't get any information about the translated page. Thus, most of the discussed
advantages
of LLM based translations cannot be materialized. It might be possible to use the process for human translations,
but that seems somewhat difficult, and augmenting the live copy process with transparent translation might prove to be
simpler for the editors in practice.

For more details you can look at the
[specification](https://github.com/ist-dresden/composum-AI/blob/84b07ece77536f7db0034a7bb8b41ddf324b06a5/featurespecs/8AutomaticTranslation.md)
.

### Differential re-translation (used in the AI Translation Merge Tool)

An interesting possibility where the reasoning capabilities of LLM could ease the translators work is the case where
an automatical translation was done and an editor made manual changes to that translation. If in such a case the
master language the text is changed and a simple automatical translation would be re-run, then the manual changes would
be
lost. When employing an LLM, it'd be possible to give it:

1. the first version of the language master
2. the text that was automatically translated to
3. the text that was manually changed into
4. the new version of the language master
   and ask the LLM to translate the new version and to replicate the manual changes between 2 and 3 into the new
   version.
   This could minimize the manual work for such cases.

If you like playing around with that -
[here is a small demonstration app](https://aigenpipeline.stoerr.net/differentialReTranslation/differentialReTranslation.html)
for the idea. (It needs an OpenAI API key to work, but you can look at some English to German examples even without
that.)

> Language models weave,\
> Context and tone they perceive,\
> Artistry achieve.\
> -- ChatGPT

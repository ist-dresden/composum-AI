# Feature specification of the automatic translation process

## Table of contents

- [Rationale](#rationale)
- [Basic idea](#basic-idea)
- [Assumed site structure](#assumed-site-structure)
- [Dealing with references to assets and similar](#dealing-with-references-to-assets-and-similar)
    - [Experience Fragments](#experience-fragments)
    - [Content Fragments](#content-fragments)
    - [Assets (images etc.)](#assets-images-etc)
    - [AEM Language copies as comparison](#aem-language-copies-as-comparison)
    - [Approach to these assets](#approach-to-these-assets)
- [Triggering the translation with a proof of concept UI](#triggering-the-translation-with-a-proof-of-concept-ui)
- [Triggering the translation as a rollout action](#triggering-the-translation-as-a-rollout-action)
    - [Open points for the rollout configuration](#open-points-for-the-rollout-configuration)
- [Some technical details](#some-technical-details)
    - [Analysis of saving the original and translated texts](#analysis-of-saving-the-original-and-translated-texts)
    - [Heuristics for translateable attribute recognition](#heuristics-for-translateable-attribute-recognition)
    - [Identification of the translation source](#identification-of-the-translation-source)
- [Open points](#open-points)
- [Development](#development)
    - [REST interface for the UI](#rest-interface-for-the-ui)
    - [Background information about live copies](#background-information-about-live-copies)
    - [Testing on WKND Site](#testing-on-wknd-site)
        - [Setup of live copies for testing a full site translation](#setup-of-live-copies-for-testing-a-full-site-translation)
- [More details](#more-details)
    - [Differential translation experiment](#differential-translation-experiment)
- [More ideas](#more-ideas)

## Rationale

While it is possible to translate item by item with the content creation dialog in AEM or the translation dialog in
Composum, it is more efficient to translate a whole page or a whole folder at once. This is especially true for the
translation of the whole website. For AEM there is a translation process ith language copies available, but it is rather
laborious to use. So we want to provide a translation process with which it is as easy as possible to create a raw
translated version of a page, page tree or site in one go, where the job of the editor is just to inspect the pages
afterwards and fix them.

## Basic idea

For AEM: instead of the language copies mechanism that is often used for site translation we employ the live copy
mechanism, which might be simpler to use, which would also be a distinguishing feature of the Composum AI translation
process. The translation would then be a live copy of the original page. The translation process would then go through
all components of the translated tree and replace the texts in the text properties. (We opt not to break the
inheritance). This would
provide an initial translation; for updating the translation, the live copy mechanism could be used in conjunction with
a translation by the content creation dialog, or the translation could be updated by the process.
Please notice that that changes the semantics of "inheritance" in live copies somewhat: if the inheritance is unbroken,
the live copy is the AI translated version of the original page, and if the inheritance is broken, the live copy 
has manual changes.

This process can be triggered separately, or with a special action performed on rollout - e.g. a new
[synchronization action](https://experienceleague.adobe.com/docs/experience-manager-65/content/sites/administering/introduction/msm-sync.html?lang=en#installed-synchronization-actions)
that can be integrated into rollout actions and triggers the translation process.

Translated would normally be properties that "obviously" contain text, like jcr:title, jcr:description, text, title
etc. (Let's search the wcm core components documentation for that), and properties that heuristically "look like text",
that is, contain multiple whitespace sequences. Since that's bound to fail sometimes, we need a rule configuration
mechanism in the OSGI configuration that defines positive / negative exceptions.

Since translation is costly in terms of money for the requests, time since ChatGPT isn't too fast, and human time since
the translation has to be checked, we want to store information in the components about the original text it was
translated from (either verbatim or in form of a hash, to see when a re-translation needs to be triggered)
and the text it was originally translated to (so that we can check whether it was manually fixed afterwards, possibly
also as a hash).

We save property values : the property value before the translation is saved with prefix `ai_original_` and the
property name, and the property value after the translation is saved with prefix `ai_translated_ and the property name.

### Assumed site structure

We assume for now an MSM site structure with:

- `/content/{site}/language-masters/{en,de,fr,...}` - the language masters for all relevant languages. There is a
  primary language master, let's say `en`. The other language masters are live copies of the primary language master.
  (That's not 100% required and is somewhat contrary to the normal usage of live copies, but makes it clear where the
  source of a translation is. That's an assumption for now.)
- /content/{site}/{en,de,fr,...} are live-copies of the language masters with the respective languages.

That's not "baked in", just the mental model for now with which we check the processes for usefulness.

## Dealing with references to assets and similar

### Experience Fragments

usage e.g.
/content/wknd/language-masters/lc/about-us ->
/content/experience-fragments/wknd/language-masters/en/contributors/stacey-roswells/master

Has the same structure as normal content with language masters and so forth.
We can make a live copy of the experience fragment folder and translate it, and then replace the items.

### Content Fragments

usage e.g.
/content/wknd/language-masters/lc/adventures/ski-touring-mont-blanc ->
/content/dam/wknd/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc

Currently heuristic recognition of text attributes. That will probably need a specific implementation observing the
models, possibly a translation of the actual models.

### Assets (images etc.)

They have titles etc, so they are language specific as well, but these are often not used. If content fragments are
used, a live copy of the assets for translating content fragments will copy image assets, too - that might be a
problem.

### AEM Language copies as comparison

Pages have properties before translation:

        cq:cloudserviceconfigs="[cloudconfigs/translation/translationcfg/default_translation,cloudconfigs/translation/msft-translation/msft_trial]"
        cq:isTransCreated="{Boolean}true"
        cq:lastTranslationUpdate="{Date}2024-02-21T10:00:56.150+01:00"
        cq:translationJob="[/content/projects/portugese/portugese/jcr:content/dashboard/gadgets/translationjob]"
        cq:translationProject="[/content/projects/portugese/portugese]"
        cq:translationSourcePath="/content/wknd/language-masters/en"
        cq:translationStatus="DRAFT"

-> Completely automatic translation is possible. I couldn't try review process yet - probably because of an error:

    21.02.2024 10:12:23.824 *ERROR* [JobHandler: /var/workflow/instances/server0/2024-02-20/sync_translation_job_3:/content/projects/portugese/portugese/jcr:content/dashboard/gadgets/translationjob] com.adobe.cq.wcm.translation.impl.I18nDictionary Could not create I18n dict for language pt at /apps/i18n/ I18n Dictionary translation should be managed at the time of development or integration

created copy of /content/dam/wknd/en to /content/dam/wknd/pt - but copied only referenced stuff.
Similarily /content/experience-fragments/wknd/language-masters/pt .

Language copies seem to be recognized by path. (Renaming a copy means it is not shown as language copy in AEM Sites.)

### Approach to these assets

We try to follow the way AEM does it - that is, we will use copies of the assets at the langage sister path. These
can be created as live copies or just copies and translated with the UI. During the page translation process we
check all paths for having a translated language sister path and replace them with the translated paths.
(Not quite sure whether it's right to cut inheritance or not.)

## Triggering the translation with a proof of concept UI

The translation process would be a long running process in the server, translating page by page. Thus, it needs to
display progress information, allow for cancellation and provide a way to inspect the results. There has to be a form to
start the translation process, and a list of translations in progress including links to the translated pages within the
editor, and details about the translation process.

Parameters for the translation process are:

- the path of the page to translate
- a `recursive` flag whether to translate all pages recursively or only the named page or resource
- a parameter `changed` flag that triggers retranslation if the original text has changed.

## Triggering the translation as a rollout action

We define a rollout configuration "Composum AI Autotranslate"
that triggers the translation process for the pages that are rolled out with that
configuration. This is an additional configuration - to get the pages copied we need e.g. the standard rollout
configuration, and then the translation rollout configuration to get the pages transparently translated.

Rollout of a component or re-enabling the inheritance of the component resets that component completely, which would
remove the properties we save our translation information in. To avoid that we break inheritance for these
properties selectively.

### Open points for the rollout configuration

- how to do that best in the background
- is there a way to avoid ordering dependence with other rollout configurations?
- how to view statistics
- configuration?
- is that triggered on initial copying?

## Workflows

There are two workflows "Composum AI Translate Page" and "Composum AI Translate Page Tree" that trigger a
translation of the page they are triggered on. They require the page is set up as a live copy of the primary language.
com.composum.ai.aem.core.impl.autotranslate.workflow.AutoTranslateWorkflowProcess defines the step.

## How about integration as Translation Provider

The 
(normal translation process)[https://experienceleague.adobe.com/docs/experience-manager-learn/sites/multi-site-management/updating-language-copy.html] 
uses language copies. Unfortunately, integrating into that process is a bit difficult. The machine translation 
process in translation connectors is heavily geared towards translating individual texts independently from the 
translated page. Ultimately this amounts to implementing the method
[TranslationService.translateArray](https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/cm/adobe/granite/translation/api/TranslationService.html#translateArray-java.lang.String:A-java.lang.String-java.lang.String-com.adobe.granite.translation.api.TranslationConstants.ContentType-java.lang.String-)
in the connector, which doesn't even get any information about the current page. Thus, most of the discussed advantages
of LLM based translations cannot be materialized. It might be possible to use process for human translations, but 
that seems somewhat difficult, and augmenting the live copy process with transparent translation might prove to be 
simpler for the editors in practice.


### Links

- https://github.com/Adobe-Marketing-Cloud/aem-translation-framework-bootstrap-connector
- Translation configurations: /libs/settings/cloudconfigs/translation/msft-translation/msft_trial ,
  /libs/settings/cloudconfigs/translation/translationcfg/default_translation
  /conf/global/settings/cloudconfigs/translation/translationcfg/default_translation
- https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/adobe/granite/translation/api/TranslationService.html
- http://localhost:4502/conf/global/settings/cloudconfigs/translation/translationcfg/default_translation.html / http://localhost:4502/mnt/overlay/cq/translation/cloudservices/tifeditor.html/libs/settings/cloudconfigs/translation/translationcfg/default_translation

## Some technical details

An easy and pretty reliable, though slow, way would be to translate each text in one request. It would likely improve
results if the whole page text was given as "background information" for the translation, but that would increase cost
several times (which might or might not be a problem).

For full page translation it's likely an advantage to put all text of the page into one request, as that automatically
provides a context for the translation. That needs precise separation of the texts and precise ordering of the
translations in the result. We put separate the texts in the original message with separators like
`%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% 573472 %%%%%%%%%%%%%%%%`
containing random numbers, and instruct ChatGPT to include the separators in the translation. (The long % sequences are
still single token sequences in the ChatGPT tokenizer.) As last separator we add 424242 to the end of the message,
allowing easy checking whether the message was translated completely. Too long messages are halved for "divide and
conquer".

We store both the original text and the translated text verbatim in the component as `ai_(propertyName)_original` and
`ai_(propertyName)_translated` respectively. That has two advantages:

- It allows easy checking whether the original (source) of the translation has changed, and thus a re-translation is
  necessary.
- It allows easy checking whether and how the translation has been manually corrected.

To allow later for a "re-translate" dialog that can provide ChatGPT with the previous original text, the text it was
translated to, the text it was manually corrected to and the text of the new original text to be translated. That might
massively improve the result if an editor has corrected the translation afterwards.

For automatically replacing paths with language copies we follow the same strategy: the original path is stored as
`lc_(attributeName)_original` and the translated path as `lc_(attributeName)_translated`. Thus we can reinstate the path
automatically if the user clicks on re-enable inheritance.

### Analysis of saving the original and translated texts

There are the following states for a property:

1. no translation done
2. original saved as blueprint and saved translation is as the text.
3. inheritance broken, text changed: saved original is as blueprint and saved translation is the old translation.
    - if the inheritance is re-enabled, the old translation is reinstated -> 2.
4. inheritance intact, original changed but not rolled out, saved original and saved
   automatic translation are out of date
    - rollout: saved original and saved automatic translation and text are discarded and updated. -> 2.
5. original changed but not rolled out, inhertance broken. saved original and saved automatic
   translation are out of date, manual changes.
    - if the inheritance is re-enabled: synchronize component would normally get the value from blueprint. Right way:
      differential-translate the blueprint, but takes time. Bad way: roll back to the old translation. -> 2. (We might
      save the manual changes, though)
    - rollout: normally no changes -> also do it this way. (Unclear: start manual process with differential
      translation?) -> still at 4.

An invariant is that if inheritance is not broken, the saved translation is equal to the text. This is only broken
temporarily during a rollout when inheritance is not broken, the text ist reset to the
blueprint's state, which has to be fixed during the execution of the translation - either by putting the result of
the previous manual translation into the text, or by re-translating the text if the blueprint has changed.

Open point: it's not clear to me when to apply the differential retranslation, nor where the manual translation
result should come from.

- During rollout probably nothing should change if inheritance is broken; if not it could be applied but what would
  be the approved manual translation?
- If the inheritance is re-enabled, the user expects a reset, so the old manual translation is probably not the
  approved translation.
  Two thinkable ways:
- Manual process (possibly supported by a workflow)
- Automatically during rollout when inheritance is cancelled - that must be switchable, though.

### Heuristics for translateable attribute recognition.

To avoid having to configure the translation for all properties we use a heuristic to recognize translated
properties that is meant to have likely no false positives and few false negatives. Specifically, we check whether the
property name is one of jcr:title, jcr:description, title, alt, cq:panelTitle, shortDescription, actionText,
accessibilityLabel, pretitle, displayPopupTitle, helpMessage , or alternatively doesn't have a colon in the name, has a
String value, doesn't start with /{content,apps,libs,mnt}/ in the value and the value has a whitespace and at least
one 4 letter sequence.

### Identification of the translation source

If e.g. `de` is a live copy of `en` then there are several possibilities when a translation is done for a property
of a component.

1. `de` has the same (english) text as `en` - initial state after live copy creation. Source doesn't matter.
2. `de` has a different text than `en` but still english. (`en` changed after live copy creation.) Unclear.
3. `de` was already translated, and the saved source is different than the current `en` source. (Normal case for
   re-translation.) Likely the source should be the current `en` text.

Since in the third case the intended source is clearly the current `en` text and in the other ones it's not clear,
we always take the current `en` text (that is, the source of the live copy) as source for the translation for now.
This corresponds to a live copy 'rollout' with integrated translation.

## Open points

- How well do languages with very different character systems (ja, es, ru, chinese zh, korean ko) work? A
  translation is performed, looks nicely and when translated back with Deepl the texts make sense, but it'd be
  interesting how skilled the translation is. Also, that might need additional instructions for the translator to
  choose the right variant, tone etc. (Simplified chinese might be zh-CN , traditional zh-Hant). We checked with 
  [Deepl Translator](https://www.deepl.com/translator) but that needs a native speaker to judge.
- Currently the translator can be switched on or off, but there is no configuration for whom it is available and no
  configuration in what content areas it can be used.
- If a manual correction was made and the translation is updated, anyway, the manual correction should be saved for
  later fixing. It's not quite clear how to collect that information, though.
- For content fragments likely the models have to be translated. That'd need some dedicated process.
- In general - how to deal with i18n of components? (Probably out of scope, but needs discussion.)
- possibl rewrite to use
  [Sling Jobs](https://medium.com/@jlanssie/translate-entires-websites-in-aem-automatically-with-openai-944875cbfa22)
  for the translation, or always use workflows
- If it's a synchronization action - how to check whether it's finished?
- Custom tool: show only conditionally?

## Links

https://www.youtube.com/watch?v=MMtS8ag6OUE - AEM automatic translation
https://experienceleague.adobe.com/docs/experience-manager-learn/sites/multi-site-management/updating-language-copy.html?lang=en -
Workflow when updating a language copy
https://experienceleague.adobe.com/en/docs/experience-manager-65/content/implementing/developing/extending-aem/extending-workflows/workflows

https://medium.com/@vsr061/create-custom-aem-menu-tools-with-granite-ui-shell-53c56e435f8a - Custom AEM menu tools
https://medium.com/@jlanssie/how-to-create-a-custom-tool-in-aem-78d14c1f66d5

Configuration of translation rules in AEM:
https://experienceleague.adobe.com/docs/experience-manager-65/content/headless/translation/translation-rules.html
https://developer.adobe.com/experience-manager/reference-materials/cloud-service/javadoc/com/adobe/granite/translation/api/TranslationService.html
https://github.com/Adobe-Marketing-Cloud/aem-translation-framework-bootstrap-connector

### Links for translation on rollout

https://github.com/AdobeDocs/experience-manager-65.en/blob/main/help/sites-administering/msm-best-practices.md etc.
https://www.linkedin.com/pulse/aem-msm-custom-rollout-action-nurbek-umirov/
https://experienceleague.adobe.com/docs/experience-manager-65/content/sites/administering/introduction/msm-sync.html?lang=en

### Links for workflows

https://medium.com/@jlanssie/translate-entires-websites-in-aem-automatically-with-openai-944875cbfa22 workflow to
translate pages. Interesting points: translates text in the for of JSON.
https://medium.com/@jlanssie/create-a-custom-workflow-model-in-aem-with-a-full-code-coverage-unit-test-4178b2263b81
https://experienceleague.adobe.com/en/docs/experience-manager-65/content/implementing/developing/extending-aem/extending-workflows/workflows

## Development

### REST interface for the UI

The REST interface needs the following operations. (URL prefix is /apps/composum-ai/components/autotranslate/list)

- List of translation processes (GET /apps/composum-ai/components/autotranslate/list.html)
    - Returns: list of links to the translation run including basic metadata (status), sorted by recency
    - HTML: form for start translation + table with 2 columns translation run status (RUNNING, DONE) incl. title with
      metadata + link
- Start translation process (POST /apps/composum-ai/components/autotranslate/list.create.html)
    - Parameters: path of the page to translate, recursive flag. (The language would be determined from the path.)
    - Returns: link to a translation run (including an id)
    - HTML: redirect to info about translation run
- Information about a translation run (/apps/composum-ai/components/autotranslate/run.html/{id})
    - Status (Started, Running, Finished, Cancelled), Start / Stop time, User
    - List of pages already translated
    - List of pending pages
    - HTML: table with status; cancel button (form), table with pages : 2 columns, DONE/PENDING, path with link to
      editor
- Cancel translation process (POST /apps/composum-ai/components/autotranslate/run.cancel.html/{id})
    - (POST) to the translation run with suffix /cancel
    - HTML: redirect to list of translation processes

We should follow HATEOAS principles - thus we might not even need an explicit user interface except the REST interface
for a start. The responses can contain HTML displayable in the browser, and even HTML forms to trigger actions.

### Background information about live copies

/content//cq:LiveSyncConfig shows the live copies - subnode of jcr:content, e.g.:

    <jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
              jcr:primaryType="cq:LiveCopy"
              cq:isDeep="{Boolean}true"
              cq:master="/content/wknd/language-masters/en">
    </jcr:root>

Resources that are in a live relationship get a `jcr:mixinTypes="[cq:LiveRelationship]"`.
Cancelled relationship: `jcr:mixinTypes="[cq:LiveRelationship,cq:LiveSyncCancelled]"` ,
`cq:isCancelledForChildren="{Boolean}true"` . The editor seems to use cq:isCancelledForChildren=true only when the
resource has no child resources. For the jcr:content node only property inheritance is cancelled - cancelling the
resource inheritance seems to put the editor into trouble.

### Testing on WKND Site

Content fragments: there are some in /content/dam/wknd/*/adventures and /content/dam/wknd/*/magazine -> for testing
contant fragment translation only these folders have to be copied.

#### Setup of live copies for testing a full site translation

- http://localhost:4502/assets.html/content/dam/wknd# : create /content/dam/wknd/de and make live copies of
  /content/dam/wknd/en/adventures and /content/dam/wknd/en/magazine into that.
- http://localhost:4502/aem/experience-fragments.html/content/experience-fragments create
  /content/experience-fragments/wknd/language-masters/de as live copy of
  /content/experience-fragments/wknd/language-masters/en
- http://localhost:4502/sites.html/content/wknd/language-masters create /content/wknd/language-masters/de as live copy
  of /content/wknd/language-masters/en

-> translate /content/dam/wknd/de , /content/experience-fragments/wknd/language-masters/de and
/content/wknd/language-masters/de


### Entry points for the translation process

#### On Rollout

apps/msm/composum-ai/rolloutconfigs/composumAiAutotranslate/.content.xml : 
served by com.composum.ai.aem.core.impl.autotranslate.rollout.AutoTranslateLiveActionImpl

If this is during live copy creation, the translation is put into the queue at AutoTranslateService.startTranslation 
since live copy relationships are not yet available. If the copy is alread there, the translation is started immediately.

The action is set to handle only jcr:content nodes since it is called for the jcr:content and it's subnodes, and 
it's difficult to deduplicate that. That has, however, the problem that a rollback of just one component doesn't
auto-translate.

## Experiment results

- when the translation is called, the duringLiveCopyCreation is now always false.
- throwing an exception in translation rolls back the whole translation - nothing is translated at all.
- Reenabling inheritance on a component with "Synchronize Component after reverting inheritance" calls 
  AutoTranslateLiveActionImpl on the component.
- Reenabling inheritance on a component with "Synchronize Page after reverting inheritance" calls 
  AutoTranslateLiveActionImpl on the page and all subcomponents. -> only component can be distinguished by the not 
  having finished(!) a call for the page in the last seconds.

## More details

### Differential translation experiment

When a text was translated and then manually corrected, and the original text was changed again, we want to include
the manual correction in the new translation. An example how this can be done is the following; load the JSON into
https://chatgpttools.stoerr.net/chatgpttools/multiplemessagechat.html for viewing.
[{"role":"system","content":"You are tasked as an expert translator to translate texts with utmost fidelity, preserving the original style, tone, sentiment, and all formatting elements (markdown, HTML tags, special characters) to the greatest extent possible.\nIMPORTANT: Only provide the translated text, maintaining all original formatting and non-translatable elements. Avoid any extraneous comments or actions not directly related to the translation."},{"role":"user","content":"Print the original text you have to translate exactly without any comments."},{"role":"assistant","content":"Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book."},{"role":"user","content":"Print this original text translated into German."},{"role":"assistant","content":"Lernen Sie unsere außergewöhnlichen Reiseführer kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind."},{"role":"user","content":"Print this original text as it was manually adapted."},{"role":"assistant","content":"Lernen Sie unsere außergewöhnlichen Reiseleiter kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind."},{"role":"user","content":"Print the new text that is to be translated."},{"role":"assistant","content":"Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book. Choose the perfect guide for yourself!"},{"role":"user","content":"Translate the new text. Take care to include the manual adaptions for the original text."},{"role":"assistant","content":"Lernen Sie unsere außergewöhnlichen Reiseleiter kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind. Wählen Sie den perfekten Reiseleiter für sich!"}]

## More ideas

- Implement differential translation
- Tool for creating translations of i18n dictionaries?
- Composum would work quite differently, but that will be an afterthought after a POC for AEM.
- Unclear: how to handle translations if the result of the translation was manually adapted?

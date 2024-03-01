# Feature specification of the automatic translation process

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
all components of the translated tree, break the inheritance and replace the texts in the text properties. This would
provide an initial translation; for updating the translation, the live copy mechanism could be used in conjunction with
a translation by the content creation dialog, or the translation could be updated by the process.

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

??? How to update the text attribute?

### Assets (images etc.)

Unclear: they have titles etc, so they are language specific as well. Copy?

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

## UI

The translation process would be a long running process in the server, translating page by page. Thus, it needs to
display progress information, allow for cancellation and provide a way to inspect the results. There has to be a form to
start the translation process, and a list of translations in progress including links to the translated pages within the
editor, and details about the translation process.

Parameters for the translation process are:

- the path of the page to translate
- a `recursive` flag whether to translate all pages recursively or only the named page or resource
- a parameter `changed` flag that triggers retranslation if the original text has changed.

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

- Do languages with very different character systems (jp, es, ru, chinese, korean) work?
- Not quite clear yet: what exactly is the problem with the AEM translation process?

## Links

https://www.youtube.com/watch?v=MMtS8ag6OUE - AEM automatic translation
https://experienceleague.adobe.com/docs/experience-manager-learn/sites/multi-site-management/updating-language-copy.html?lang=en -
Workflow when updating a language copy

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

## More details

### Differential translation experiment

When a text was translated and then manually corrected, and the original text was changed again, we want to include
the manual correction in the new translation. An example how this can be done is the following; load the JSON into
https://chatgpttools.stoerr.net/chatgpttools/multiplemessagechat.html for viewing.
[{"role":"system","content":"You are tasked as an expert translator to translate texts with utmost fidelity, preserving the original style, tone, sentiment, and all formatting elements (markdown, HTML tags, special characters) to the greatest extent possible.\nIMPORTANT: Only provide the translated text, maintaining all original formatting and non-translatable elements. Avoid any extraneous comments or actions not directly related to the translation."},{"role":"user","content":"Print the original text you have to translate exactly without any comments."},{"role":"assistant","content":"Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book."},{"role":"user","content":"Print this original text translated into German."},{"role":"assistant","content":"Lernen Sie unsere außergewöhnlichen Reiseführer kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind."},{"role":"user","content":"Print this original text as it was manually adapted."},{"role":"assistant","content":"Lernen Sie unsere außergewöhnlichen Reiseleiter kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind."},{"role":"user","content":"Print the new text that is to be translated."},{"role":"assistant","content":"Meet our extraordinary travel guides. When you travel with a certified WKND guide you gain access to attractions and perspectives not found on the pages of a guide book. Choose the perfect guide for yourself!"},{"role":"user","content":"Translate the new text. Take care to include the manual adaptions for the original text."},{"role":"assistant","content":"Lernen Sie unsere außergewöhnlichen Reiseleiter kennen. Wenn Sie mit einem zertifizierten WKND-Reiseleiter unterwegs sind, erhalten Sie Zugang zu Attraktionen und Perspektiven, die nicht auf den Seiten eines Reiseführers zu finden sind. Wählen Sie den perfekten Reiseleiter für sich!"}]

## More ideas

- We might give the user a way to provide general information about the translation and the site - perhaps extending the
  system message.
- Implement differential translation
- Tool for creating translations of i18n dictionaries?
- Composum would work quite differently, but that will be an afterthought after a POC for AEM.
- Unclear: how to handle translations if the result of the translation was manually adapted?
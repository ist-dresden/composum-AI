# Rethinking automatic translation in AEM with LLMs

## A proof of concept and preliminary implementation

Translating a site is often a rather large effort. Since modern LLM can both speak many languages and integrate
advanced reasoning capabilities, they can be a big help in translating a site - possibly to the point that 
translating a site can amount to an automatic process where the editor only has to check the results and only 
occasionally make minor changes.

We integrated a proof of concept into the Composum AI that can translate individual pages or entire sites quickly.
It is very promising: a test translating the [WKND site](https://wknd.site/us/en.html) from English into German took
only about 10 minutes,
and the translation showed little need for manual corrections. Translating into other languages like Spanish, Japanese,
Chinese, Korean seem to work nicely as well, as far as we can judge by translating the text back with
[Deepl Translator](https://www.deepl.com/translator) .

Also, we implement a novel process of integrating the translation transparently into the rollout process of live
copies, which allows for very easy updating of the translated sites whenever changes are made in the primary language
site.

It's quick and easy to try that on your own site - install it locally or on one of your test systems and check out
the result!
Please let us know what you think and how it works for you and contact us on any trouble!

## A quick demo

<a href="../image/ai/video/AEMAutoTranslation.mp4" target="_blank">
<figure>
<img src="../image/ai/video/AEMAutoTranslatePoster.png" alt="Composum AI for AEM automatic translation Quick Demo" 
width="500px"/><br/>
<figcaption>
Quick demonstration of automatic translation of an AEM site with Composum AI
</figcaption>
</figure>
</a>

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
live copies are just made for that: change a page and hit "rollout" and the changes are transferred. So our
translation process integrates transparently into the rollout process: an additional rollout configuration
translates all texts into the language configured for the rollout target during the rollout action.

## Usage

There are currently three ways to use the prototype (see below):

- a mini UI that can be used to translate pages / page trees and roll back the translations. That can be used to
  easily try out the results.
- two workflows for translating a page or page tree from its blueprint
- an additional rollout configuration that transparently translates the rolled out page during rollout

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
  and then (only after that!) the "Composum AI Autotranslate POC" rollout configuration.

Then the pages will be transparently be translated on each rollout.

For the initial live copy creation we recommend to

1. set up the live copy with the right language setting,
2. try the translation using the workflow or the POC UI on a few pages, and
3. then add the rollout configuration to the root of the tree to be automatically translated.

## Triggering translation with the workflows

There are two workflow models "Composum AI Translate Page" and "Composum AI Translate Page Tree" that trigger a
translation of the page they are triggered on, resp. the page tree. They require the page is set up as a live copy of
the primary language.

## Usage of the POC UI

For quick experimentation and evaluation, we provide a very simple proof of concept style UI. The entry point for the
translation is the URL in the author server
[/apps/composum-ai/components/autotranslate/list.html](http://localhost:4502/apps/composum-ai/components/autotranslate/list.html)
It allows entering a path for a page or content / experience fragment or whole site and starting a translation process
for either that page only, or for that page and it's subpages.

Depending on the size of the site the translation can
take a couple of minutes - a list of triggered translations is provided, providing links to detail views with the
actual pages that were translated and the status / statistics of the translation. The detail view also provides a
button to cancel and undo the translation.

The list view also provides a form to quickly roll back the translation for a subtree for quick experimentation.
This will also reenable inheritance of all properties and resources, and reset the
translated texts to the state before the automatic translation.

## Installation / Configuration

The proof of concept is integrated into the Composum AI version >= 0.8.0. The POC UI has to be enabled by OSGI
configuration. If you are newly installing the Composum AI:
it needs at least [configuration of the API key](configuration.md) for Composum AI in general; if you want to use
the POC UI you need to enable that in the "Composum AI Autotranslate Configuration"
(PID `com.composum.ai.aem.core.impl.autotranslate.AutoTranslateServiceImpl`) .

If the heuristics to determine the translated resources / properties are not sufficient for your site, you configure
exceptions in an OSGI configuration "Composum AI Autotranslate Configuration"
(see the [configuration reference](../autogenerated/osgiconfigurations.html#osgi.AutoTranslateConfig) ).

Additionally, there is a Sling contextaware configuration "Composum AI Automatic Translation POC Configuration"
that can be used to add additional instructions how to translate the pages in the subtree the configuration applies to
(compare [sling configuration reference](../autogenerated/slingcaconfigurations.html#slingca.AutoTranslateCaConfig) ).

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
- The [standard AEM translation process](https://experienceleague.adobe.com/docs/experience-manager-learn/sites/multi-site-management/updating-language-copy.html) 
  uses language copies. Besides the discussed advantages of live copies, integrating into that process is a bit 
  difficult. The machine translation process in translation connectors is heavily geared towards translating 
  individual texts independently from the translated page. Ultimately this amounts to implementing the method
  [TranslationService.translateArray](https://developer.adobe.com/experience-manager/reference-materials/6-5/javadoc/com/adobe/granite/translation/api/TranslationService.html#translateArray-java.lang.String:A-java.lang.String-java.lang.String-com.adobe.granite.translation.api.TranslationConstants.ContentType-java.lang.String-)
  in the connector, which doesn't get any information about the translated page. Thus, most of the discussed
  advantages
  of LLM based translations cannot be materialized. It might be possible to use the process for human translations, 
  but that seems somewhat difficult, and augmenting the live copy process with transparent translation might prove to be
  simpler for the editors in practice.

## Possible extensions

<a id="difftranslation"></a>

### Differential re-translation

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

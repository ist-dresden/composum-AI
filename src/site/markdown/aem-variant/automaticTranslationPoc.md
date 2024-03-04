# Rethinking automatic translation in AEM with LLMs, proof of concept

Translating a site is often a rather large effort. Since modern LLM can both speak many languages and integrate
advanced reasoning capabilities, they can be a big help in translating a site - to the point that translating a site
can amount to an automatic process where the editor only has to check the results and only occasionally make minor
changes.

We integrated a proof of concept into the Composum AI that can translate individual pages or entire sites quickly.
It is very promising: a test translating the [WKND site](https://wknd.site/us/en.html) from English into German took
only about 10 minutes,
and the translation showed little need for manual corrections. Translating into other languages like Spanish, Japanese,
Chinese, Korean seem to work nicely as well, as far as we can judge by translating the text back with
[Deepl Translator](https://www.deepl.com/translator) . You are invited to try it out and let us know what you think!

## Advantages of our approach using of LLM for translation

While being employable for providing quick and inexpensive machine translation, LLMs like ChatGPT offer advanced
reasoning capabilities that can improve the quality of the translation:

- We collect the texts of one page, experience fragment or content fragment and present them together in one
  translation request to ChatGPT. Thus, it can understand the context of the text and translate it accordingly.
- By default, the LLM is instructed to be close to the original text, preserving the original style, tone, sentiment,
  and formatting. But:
- Additional instructions can be given to influence the writing style, finetuning the translation
  to the desired audience, and even to give background information about the site and the translation, if needed.
  That can range from minor changes like influencing how formal to address the user to major modifications like
  simplifying the language for a younger audience.
- While that's not currently implemented, we make preparations so that manual corrections of translations can be
  preserved when a retranslation of a modified text in the original language is done (see ["Differential 
  re-translation"](#difftranslation) below).

Or course, the full power of the [Composum AI](usage.md) can then be used to further improve the translated texts.

## Assumed site structure and the translation process

Such an automatic translation process is especially useful for sites where the different language versions are very
close to each other in content and structure - mostly the language is diffent. An interesting point about the
prototype is that it currently requires that the other languages are live copies of their primary language. Thus,
changes in the master language can be rolled out to the other languages, and the automatic translation takes care of
the translation of the changed / new texts. For each translated text, the inheritance is broken so that the text can
be replaced automatically and manually edited, if needed.

As an example, for the WKND site the structure would look like this (just mentioning languages `en` and `de` and
countries `us` and `de` for simplicity):

- `/content/wknd/language-masters/en` - the primary language master
- `/content/wknd/language-masters/de` is a _**live copy**_ of the primary language master, auto-translated. This is a
  difference to the normal usage of live copies, but makes it clear where the source of a translation is and rolling
  out changes to the other languages is easy.
- `/content/wknd/us/en` and `/content/de/de` are the resp. country sites, live copies of the language masters. These
  are mostly unmodified copies of the language master plus some country-specific content. (Of course, for
  multilingual sites that do not have a country-specific structure, such country sites could be omitted.)

A similar structure is used for experience fragments (`/content/experience-fragments/wknd/language-masters`) and
content fragments (`/content/dam/wknd`). These have to be translated first - the automatic translation process
also replaces links to `en` fragments with links to `de` fragments if they exist.

Whenever changes are introduced into the english language master, these can be rolled out to the other languages
since these are live copies. Thus, new texts would initially appear in english. A run of the automated translation
would translate these, as well as re-translate text that were changed in the english language master after the last
translation. A list of changed pages allows manually checking the translation before they can be rolled out to the
actual country sites.

## Installation / Configuration

The proof of concept is integrated into the Composum AI version >= 0.8.0, but has to be enabled by OSGI
configuration. It needs at least [configuration of the API key] for Composum AI in general, as well as saving an
empty configuration "Composum AI Autotranslate Configuration"
(PID `com.composum.ai.aem.core.impl.autotranslate.AutoTranslateServiceImpl`)
which currently has no settings except an optional switch that can be used to disable the translation.

## Usage of the proof of concept UI

Since this is a proof of concept, the UI is very simple. The entry point for the translation is the URL in the author
server
[/apps/composum-ai/components/autotranslate/list.html](http://localhost:4502/apps/composum-ai/components/autotranslate/list.html)
It allows entering a path for a page or content / experience fragment or whole site and starting a translation process
for either that page only, or for that page and it's subpages.

Depending on the size of the site the translation can
take a couple of minutes - a list of triggered translations is provided, providing links to detail views with the
actual pages that were translated and the status / statistics of the translation. The detail view also provides a
button to cancel and undo the translation.

The list view also provides a form to quickly roll back the translation for a subtree for quick experimentation.
This will reenable inheritance for the translated properties and reset the translated texts to the state before the
automatic translation.

## Current limitations / open points

- The identification of properties that need translation is currently heuristic. Standard attributes like `jcr:title`,
  `jcr:description`, `text`, `title`, `alt`, `cq:panelTitle`, `shortDescription`, `actionText`, `accessibilityLabel`,
  `pretitle`, `displayPopupTitle`, `helpMessage` are translated if they contain word characters. For other properties
  we check that they are not paths, contain whitespace and contain some word characters. That would exclude
  nonstandard properties containing single words from translation, and might lead to some false positives. For
  productive usage adding configuration for positive and negative exceptions would likely be needed.
- At the moment the translator can only be switched off or on, but cannot be restricted to certain users or content
  areas.
- The list of performed translations is not persisted, so it is lost when the server is restarted or redeployed.

## Possible extensions

<a id="difftranslation"></a>
### Differential re-translation

An interesting possibility where the reasoning capabilities of LLM could ease the translators work is the case where 
an automatical translation was done and an editor made manual changes to that translation. If in such a case the 
master language the text is changed and a simple automatical translation would be re-run, then the manual changes would be
lost. When employing an LLM, it'd be possible to give it:
1. the first version of the language master
2. the text that was automatically translated to
3. the text that was manually changed into
4. the new version of the language master
and ask the LLM to translate the new version and to replicate the manual changes between 2 and 3 into the new version.
This could minimize the manual work for such cases.

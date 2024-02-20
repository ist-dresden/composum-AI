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

## UI

The translation process would be a long running process in the server, translating page by page. Thus, it needs to
display progress information, allow for cancellation and provide a way to inspect the results. There could be a form to
start the translation process, and a list of translations in progress including links to the translated pages within the
editor.

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

### Necessary files

We implement the REST interface with HTL (e.g. /apps/composum-ai/components/autotranslate/list/list.html for the list
request)
and a Sling Model
`com.composum.ai.aem.core.impl.autotranslate.AutoTranslateListModel`

We need the following files: to implement all the requests:

- /apps/composum-ai/components/autotranslate/list/list.html for the list GET
- /apps/composum-ai/components/autotranslate/list/create.POST.html for the create POST.
- /apps/composum-ai/components/autotranslate/run/run.html for the run GET
- /apps/composum-ai/components/autotranslate/run/cancel.POST.html for the cancel POST

## Some technical details

An easy and pretty reliable way would be to translate each text in one request. It would likely improve results if the
whole page text was given as "background information" for the translation, but that would increase cost several times (
which might or might not be a problem).

If we put all text of the page into one request, that would automatically provide a context for the translation, but
needs testing since that needs precise separation of the texts and precise ordering of the translations in the result.
An idea would be to separate the texts in the original message with separators like `===<<<### 573472 ###>>>===`
containing random numbers, and instruct ChatGPT to include the separators in the translation.

## Possible improvements

Storing both the original text and the translated text verbatim in the component would allow for a "re-translate"
dialog that can provide ChatGPT with the previous original text, the text it was translated to, the text it was manually
corrected to and the text of the new original text to be translated. That might massively improve the result. Possibly
one could automatically extract instructions for the next translation process from the manual corrections.

We might give the user a way to provide general information about the translation and the site - perhaps extending the
system message.

## Open points

How would we deal with experience fragments / content fragments / image alt texts? A live copy of these would change the
path, and we'd need to update the path in the translated page. Perhaps there is already an AEM mechanism so that e.g.
one experience fragment can have several languages.

Composum would work quite differently, but that will be an afterthought after a POC for AEM.

Unclear: what exactly is the problem with the AEM translation process?

## Links

https://www.youtube.com/watch?v=MMtS8ag6OUE - AEM automatic translation

## Development

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
resource has no child resources.

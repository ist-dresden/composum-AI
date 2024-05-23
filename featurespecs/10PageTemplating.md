# Feature specification for page templating

## Background

A page in Composum Pages consists of various components where many of them contain text attributes. For instance a
teaser for another page (containing a title, subtitle and a text) or a text component that has a subtitle and a text, or
a section component that contains a title and other components. Components are done with Apache Sling in Jackrabbit JCR.

## Basic idea

In many sites (e.g. product sites) the same page structure is repeated over and over again, but with changing texts
/ images. For example, a product page could consist of a title, an attention grabber, an introduction, a list of
features, a call to action, or the AIDA framework. It would be nice if a suggestion for all that could be generated in
one go, with the same basic prompts.

Of course, the presented information has to come somewhere - as a first step, we give one or more URL from which the
text content is gathered (like in the URL source of the content creation dialog), though the information could also come
from other sources.

Since the pages can have different structures and different components, we can create an (unpublished) page template
and put the prompt and URL information into the very fields that should be filled by the AI. The user can then
copy the template, change the URL information and trigger an action on that page (in the case of AEM e.g. a workflow)
that replaces the fields with the generated content.

To support manual editing, we might save the prompts and URL(s) on the components / page and use that when opening the
content creation dialog for the first time. (Although it cannot yet do several URLs - perhaps append them with space
or | for the URL field?)

The fields in the template should contain a special marker that marks them as prompt (as some fields might not be
replaced) which is unlikely to occur in normal text. The marker might also contain an ID for the field so that other
prompts can reference to that field (e.g. "Headline for paragraph #overview"). The URL can also be given in such a
marker in any text field on the page. All that is collected and sent to ChatGPT in one go, so that it has some overview
over the text flow on the page.

## Ideas for the markers

- A field that is a prompt begins with `PROMPTFIELD: `
- A field that is referenced by other fields begins with `PROMPTFIELD#ID: ` where ID is a unique identifier for the
  field
- A URL source is added as `SOURCEURL(https://example.com/)` after that.
- A prompt that applies to the whole page can be put into a multi line field; it begins on a line with `PAGEPROMPT: `

## Format that is sent to ChatGPT

We go through the page and collect all fields that contain whitespace (possibly excluding some special fields like
the translation fields). Everything is sent to ChatGPT in the order it's in the JCR, but we want only the fields back
that contain a prompt marker. The prompt markers are replaced by the generated text. The URL markers are collected
and their text is added as background information.

Thus, we send a JSON to ChatGPT and expect a JSON back.

### Example

Page structure:

- Title: "PROMPTFIELD#NAME: name of the product"
- Text: "PROMPTFIELD: single sentence invitation to check out the product"
- Title: "Key Features"
- Text: "PROMPTFIELD: markdown list of key features"
- Title: "PROMPTFIELD: 'Why ' and the content of field #NAME'
- Text: "PROMPTFIELD: call to action"

The interesting question is how to put that into a JSON so that all texts and prompts are there, it's clear what's a
prompt and what just an informational text, and how the response and general prompt should look like so that the output
only contains the results of the prompts, but is easily matched to the input. The JSON contains only the prompts and
the text fields, not the sources and content of the sources.

#### Idea of the request

```
Create a text for a web page that is based on the retrieved information according to the following instructions which are separated into several parts. The separators like "%%%%%%%% ID %%%%%%%%" should be printed as they are, to structure both the output and the instructions.

%%%%%%%% PROMPT#PRODUCTNAME %%%%%%%%
Print as plain text: the name of the product
%%%%%%%% PROMPT#001 %%%%%%%%
Print as plain text: a 160 character SEO description for the generated web page.</p>
%%%%%%%% PROMPT#005 %%%%%%%%
Print as rich text HTML:  a brief overview what the product is. One paragraph.</p>
```

## Implementation remarks

We save the prompts on the page in the JCR, so that it can be reset to the original prompts / the process can be
repeated.

[10PageTemplatingExample.json](samples/10PageTemplatingExample.json) is an example JSON request to create a page 
from a template (tryable e.g. with https://chatgpttools.stoerr.net/chatgpttools/multiplemessagechat.html).

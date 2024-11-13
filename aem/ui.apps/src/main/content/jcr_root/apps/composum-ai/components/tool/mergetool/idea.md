# Merge tool for existing / new translations

## Background

We have an automatical translation of AEM pages. It collects all translateable texts from the source pages
components and translates them into the target page (a live copy of the source page).
The texts can be simple text attributes (fields like headlines, titles etc.),
one or a few paragraphs of plaintext for descriptions etc., and also richtext fields of one or more paragraphs.

In the target page, the translations are normally overwritten when the corresponding field has changed in the source
page or when the configuration of the translation has changed (incl. some additional instructions that can be given
for the page). However, if the inheritance of the field or the component with the field is broken, the field remains
unchanged during the rollout. This is done when translations have to be adapted manually to improve the translations.

## The problem

If a previous version of a source page has already been translated into the target page and now there is a new version
of the source page, but some inheritances have been broken, the translation process does nothing for these fields.
However, simply reenabling the inheritance is often not quite desirable, because the translation would be
overwritten by the new translation and all manually changed content would be lost. So we need a way to merge the
existing translations with the new translations.

## Idea for a merge tool

### Available information

We store for each field:

- the original text it was translated from
- the AI translated text

This is used in the translation process so that fields where the original value was not changed are not translated
again but just keep the same text. (They are however translated anyway if the translation configuration changed).

For fields that have broken inheritance we can also add for the benefit of the merge tool:

- the new text from the source
- the AI translation of that new text

### UI of the merge tool

The merge tool should list the translated / translateable fields from a page and supply the information the editor
needs to adapt the translations. (It could either display all fields or just those with cancelled inheritance, or
just changed fields.) It should have the following columns, with a row for each field:

- current (= new) source text
- current AI translation
- mini column without title that contains a ">" button to copy the current AI translation to the new translation
  field and below that a "<<" button that appends it
- editable field for the new translation
- mini column without title that contains a "<" button to copy the current target text to the new translation field
  and below that a "<<" button that appends it
- current target text as shown in the page

Optional information that might be shown in some place, but not be prominent:

- component path / field name
- Link to the source page and target page
- original source text used for the current target text

General structure:

- Headline "Translation merge tool"
- Links to the source page and target page
- A table with a header row and a row for each field. The columns are as described above.

### Mockup

PROMPT: an HTML mockup of the merge tool UI - one HTML page with embedded CSS and Javascript using bootstrap.

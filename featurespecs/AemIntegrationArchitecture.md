# Architecture of the AEM integration of Composum AI

This records the general decisions about the AEM integration - see [Ideas for AEM Integration](AEMIntegrationIdeas.md)
for the discussion of various ideas and variants.

## Scope

In comparison to the Composum variant of Composum AI we will not port the page category dialog to AEM, since in AEM 
tags are objects maintained in the JCR repository and work very much different, and also not the translation dialog, 
since there are various tools to manage AEM translations and the way of working with multilingual sites is also 
rather different to the Composum system.

The AEM variant does thus provide the Content Creation Dialog and the Side Panel AI. 

- The Side Panel AI is added to 
the left side panel in the page editor, content fragment editor and experience fragment editor, and provides a 
possibility for discussion with the AI. The user can select whether it's provided with a text representation of the 
  shown page (or fragment), the selected component or just discuss without giving any additional input.
- The Content Creation Dialog is integrated into several places where textareas or richtext editors are shown:
    - text areas in component dialogs and content fragments (icon at the label besides the help icon)
    - the toolbar of a richtext editor in pages, component dialogs, experience fragments or content fragments 
      (additional icon in the toolbar)

## Content Creation Dialog integration points

AEM specific points are:
- Integration into labels in rendered dialogs: textarea and richtext editor
- Integration into toolbar in richtext editor embedded into the page / content fragment / experience fragment
- Integration into textarea label in content fragment

Important for all these points are:
1. trigger for the hook that adds the icon and the binding
2. how determine path of field / selected component / page/content fragment/experience fragment
3. how to read content of textarea / richtext editor
4. how to write content of textarea / richtext editor

An additional complication are that there are several richtext editor components referring to several 
different richtext editors available. 

### Triggers

For opening a dialog we can register for event "coral-overlay:open" at the `$(document)`. 
Also event "foundation-contentloaded" might be useful for reacting to changes (like inserting a new richtext 
component), so we'll use it as well with insertion of the icons, if they aren't there yet.

### Side Panel AI

On a 'cq-layer-activated' after Coral.commons.ready we load the dialog and insert it into the side panel.

### textarea in dialog

We register for event "coral-overlay:open" at the `$(document)`. It get's the coral-dialog as argument, where we can 
look for textareas `div.coral-Form-fieldwrapper textarea.coral-Form-field` in it after Coral.commons.ready on the 
dialog. The path of the element is `$(textarea).closest('form').attr('action')` , attribute is the `name` of the textarea.

### Rich text editor in dialog

It seems normally the toolbar is initially visible, so we can already add the creation dialog button on 
`coral-overlay:open`.
In the Content Creation Dialog we trigger an `foundation-contentloaded` event, which activates the richtext editors. 

## Dialog rendering

We use the AEM standard way as far as possible. That means we use for a pop up dialog like the Content Creation
Dialog a TouchUI Dialog (Coral 3). It will be triggered from Javascript, though, so it can likely be a static URL.
Since we need a place to store the prompts, anyway, we render it at a resource at
/conf/composum-ai/settings/dialogs that will have subnodes with the prompt information. This way we could create 
several configurations for different sites later.

http://localhost:4502/mnt/override/apps/composum-ai/components/contentcreation/_cq_dialog.html/conf/composum-ai/settings/dialogs/contentcreation

## Javascript structure

We will conform to the Javascript handling used AEM archetype and use ES6 modules for AEM specific code. If it 
happens that some of the code could be used by both the Composum and AEM variant, we'll use the IIFE pattern used in 
Composum, since ES6 module support wouldn't be easy to use in Composum as it'd need a separate frontend build since 
the modules would have to be packed for use with Composum client libraries. It seems fine to use Javascript classes, 
though / that works in the Composum build system, too.

## Structure of responses

To make things compatible with the Composum Pages variant of Composum AI: if we generate a JSON response we generate 
it in the same way as in Composum: e.g.
{
    "status": 200,
    "success": true,
    "warning": false,
    "data": {
        "result": {
            "streamid": "7190c15e-bd98-4ceb-bd45-5e800b6370f0"
        }
    }
}
or in the error case:
{
  "status": 400,
  "success": false,
  "warning": false,
  "title": "Error",
  "messages": [
    {
      "level": "error",
      "text": "error",
      "rawText": "error",
      "arguments": [
        "arg1",
        "arg2"
      ],
      "timestamp": <timestamp>
    }
  ]
}

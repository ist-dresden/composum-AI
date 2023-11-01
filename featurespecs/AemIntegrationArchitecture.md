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

### General handling of a richtext editor

For richtext editors we want to add the content generation button to the toolbar, but cannot go the normal way of
integrating it into the richtext editor configuration because it should work everywhere without modifying the
application components for now. So we have to do a rteinstance.suspend() before opening the content creation dialog,
and to a rteinstance.reactivate() and rteinstance.focus() afterwards to open it again. That means, however, we have
to handle the dialog closing for ourselves (avoid event propagation), since that interferred with the open richtext
editor.

### Side Panel AI

On a 'cq-sidepanel-loaded' after Coral.commons.ready we load the dialog and insert it into the side panel.

### textarea in dialog

We register for event "coral-overlay:open" at the `$(document)`. It get's the coral-dialog as argument, where we can
look for textareas `div.coral-Form-fieldwrapper textarea.coral-Form-field` in it after Coral.commons.ready on the
dialog. The path of the component is `$(textarea).closest('form').attr('action')` , attribute is the `name` of the
textarea. For permission checking, the resource type is in an input with name ./sling:resourceType in the closest
coral-dialog-content. (Function insertCreateButtonsForTextareas in registerdialogs.js)

### Richtext editor in dialog

It seems normally the toolbar is initially visible, so we can already add the creation dialog button on
`coral-overlay:open` on the coral-dialog.
The button is integrated into all `.rte-ui > div > coral-buttongroup`. From the buttongroup
the actual editor can be found with `$(buttongroup).closest('.cq-RichText').find('.cq-RichText-editable').data
('rteinstance')` and the path of the component is `$(textarea).closest('form').attr('action')` , attribute is the
attribute `name` at the editor. For permission checking, the resource type is in an input with name ./sling:resourceType
in the closest coral-dialog-content.
(Alternative: an editing-start event is triggered before that on each rte div.)

BTW: In the Content Creation Dialog we need to trigger an `foundation-contentloaded` event, which activates the
richtext editors.

### Richtext editor in content (normal text component)

The toolbar is not visible until the user clicks on the editor. Thus, we have to listen for the `editing-start` event;
we register the hook for that on cq-layer-activated. The `editing-start` has as target the content element, so we
cannot use that to find the editor which lives in another frame. We ignore that and search for the `.rte-ui > div >
coral-buttongroup` in $(document) instead, and add buttons if they aren't there and register click events for them.
It seems, however, difficult to find data about the edited element, so we save that `lastEditorStartTarget`
during the `editor-start` event. It is also the carrier of the RTE
object: `$(lastEditorStartTarget).data('rteinstance')`

### Richtext editor in content fragment

The toolbar is already visible. We can register the buttons on the foundation-contentloaded event. The richtext
editor object is to be found at
`$(buttongroup).closest('[data-form-view-container=true]').find('[data-cfm-richtext-editable=true]').data('rteinstance')`
and the path is in a form action.

### Dialog rendering

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

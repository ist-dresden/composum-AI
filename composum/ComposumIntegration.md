# Details about the Composum integration of the module

As discussed in [Project Structure](./ProjectStructure.md), the composum specific code is contained in module composum.

State: in execution; this just preliminary.

## Location of integration in the UI

Over a widget we have at the left a title and at the right a description (hint). We will add buttons at the right of
the hint.

## Mechanism for embedding buttons into widget labels:

### Composum Pages side

Implemented in Composum Pages https://github.com/ist-dresden/composum-pages/pull/72 :

- PagesPlugin interface allows other projects to register, implemented in ChatGPTPagesPlugin
- a PagesPlugin.getWidgetLabelExtensions() gives a number of Sling Resourcetypes that are used to render any label
  extensions. The resource used for rendering is the property resource of the extended widget, e.g.
  /content//some/page/jcr:description . label.jsp gets sling:call to labelextension.jsp which does sling:include for
  that.
- /libs/composum/pages/stage/edit/js/dialogs.js contains a hook so that plugins can bind themselves into dialogs:
  composum.pages.dialogs.const.dialogplugins is a list of objects where dialogInitializeView is called with dialog
  and the root $element of the dialog.

### Implementation of the buttons in the widget labels in composum-chatgpt-integration

- For our implementation we use the resource type
  "composum/ai/pagesintegration/widgetextensions/labelextension"
- /libs/composum/ai/pagesintegration/widgetextensions/labelextension/labelextension.jsp implements the label
  extensions, drawing on the model model.com.composum.ai.composum.bundle.ChatGPTLabelExtensionModel for visibility
  checking of the individual buttons.
- /libs/composum/ai/pagesintegration/css/widgetextensions.scss (and variables.scss and mixins.scss copied from
  pages) for styling of the label extension.
- /libs/composum/ai/pagesintegration/js/chatgpt.js contains provisions for binding the label extension buttons
  to open the dialogs: registration in composum.pages.dialogs.const.dialogplugins so that dialogInitializeView is
  called after the dialog is rendered.

### Widget extension in detail:

labelextension calls a service (PagesPluginService) that has PagesPlugin registered
PagesPluginService is available from AbstractModel

## Relevant files in Composum Pages

### CSS

Location widgets definitions in Pages:
pages/commons/package/src/main/content/jcr_root/libs/composum/pages/commons/css/widgets.scss
in category:composum.components.widgets[css:/libs/composum/nodes/commons/components/clientlibs/components]

### Javascript

category:composum.components.widgets[js:/libs/composum/nodes/commons/components/clientlibs/components]
relevant: pages/stage/package/src/main/content/jcr_root/libs/composum/pages/stage/edit/js/dialogs.js declares many
dialogs. We need integration into create dialog, too - at least for content creation; translation doesn't matter
(would be inactive, anyway). Base class: ElementDialog, covers everything. Binding of actions seems usually done in
initView .

### Possible Icons for label extensions

Translation: https://fontawesome.com/v4/icon/language fa-language

Tagging: https://fontawesome.com/v4/icon/tags fa-tags

Content creation: perhaps https://fontawesome.com/v4/icon/align-left fa-align-left
perhaps https://fontawesome.com/v4/icon/plus-square fa-plus-square
https://icons.getbootstrap.com/icons/pencil-square/ <i class="bi bi-pencil-square"></i>
perhaps https://icons.getbootstrap.com/icons/chat-dots-fill/
! https://icons.getbootstrap.com/icons/magic/ <i class="bi bi-magic"></i>
! https://fontawesome.com/v4/icon/magic <i class="fa fa-magic" aria-hidden="true"></i>

### Handling of HTML

We have 3 cases for the widgets we support.

#### text fields as in headlines.

These are plain text. Example: this is a <b>title with bold text</b> with *emphasized* stuff.
Translation: We set the fields in Javascript with .textContent=... and escape it properly with the cpn:text tag in JSPs.
Content creation: response field is text area -> OK.

#### text areas, as in code component

No translation for code available. Content creation: same as text fields -> OK.

#### richtext editors, as in the normal text component

Translation: displayed as HTML using cpn:text type="rich" or as .html() from the json response -> OK. 
Transported to GPT as it is, got back as it is. Inserted with widget.setValue which uses it as HTML.

Content creation: we use a richttext widget with a trumbowyg editor -> value can be transferred to original widget.
Transported to GPT as is, that is as HTML. Probably OK - we'll see.

## Further ideas:

https://alex-d.github.io/Trumbowyg/documentation/ custom buttons for trumbowyg

# To review with Ralf

- handling of variables / mixins?
- Review pages integration on pages side

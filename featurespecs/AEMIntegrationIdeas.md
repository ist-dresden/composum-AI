# Ideas for AEM Integration

## Testsetup

AEM Cloud SDK with WKND Site https://wknd.site/

## Discussion how to develop the frontend

Two basic options: use ui.frontend or do it directly in the ui.apps client libraries .

Important aspect: we want the AEM and Composum FE to diverge as little as possible, so we'll need to share
javascript code as far as possible. The styling should however be similar to the native styling.

### Directly develop in client libraries

- use Immediately-Invoked Function Expressions (IIFE) like in composum
- less moving parts than ui.frontend, though the archetype functionality seems to work fine there.
- for development use automatic sync - works fine

### ui.frontend

- Can use ES6 modules or IIFE; for IIFE it's not clear
- development server

Advantages:

- a standard way for AEM projects

## Creation of the dialogs

Possible options:

- directly as HTML from AEM
- create a component with a dialog and grab the HTML from that, save that.
- create content resource that renders as the dialog
- render servlet as in Composum

Rendering dialog e.g. request
http://localhost:4502/mnt/override/apps/composum-ai/test/components/contentcreation/_cq_dialog.html/content/wknd/language-masters/composum-ai-testpages/jcr:content/root/container/helloworld2?resourceType=composum-ai%2Ftest%2Fcomponents%2Fcontentcreation&_=1692791148487

## Where to integrate

Text:
http://localhost:4502/editor.html/content/wknd/us/en/about-us.html
Content fragments:
http://localhost:4502/editor.html/content/dam/wknd/en/magazine/la-skateparks/ultimate-guide-to-la-skateparks
Core components:
https://experienceleague.adobe.com/docs/experience-manager-core-components/using/introduction.html?lang=en

### Testpages

http://localhost:4502/editor.html/content/wknd/language-masters/composum-ai-testpages.html
http://localhost:4502/editor.html/content/experience-fragments/wknd/us/en/adventures/adventures-2021/master.html
http://localhost:4502/editor.html/content/dam/wknd/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc
for testing textarea in content fragment: change to textarea
http://localhost:4502/bin/browser.html/conf/wknd/settings/dam/cfm/models/adventure/_jcr_content/model/cq%3Adialog/content/items/1570129198953

## Resource -> approximate markdown transformation

Special resource types of WKND:

- OK: wknd/components/contentfragment -> core/wcm/components/contentfragment/v1/contentfragment : fragmentPath ,
  variationName, elementNames , ??? childnodes
    - fragmentPath references /content/dam/* ; take attribute elementNames from (fragmentPath)/jcr_content/data/
      (variationName or master)
    - evtl. text attribute if present?
    - example http://localhost:4502/bin/browser.
      html/content/wknd/us/en/adventures/colorado-rock-climbing/_
      jcr_content/root/container/container_fixed/container/contentfragment
- OK: wknd/components/experiencefragment : fragmentVariationPath , ??? childnodes
- OK: wknd/components/teaser -> core/wcm/components/teaser/v1/teaser -> core/wcm/components/image : descriptionFromPage
  , titleFromPage , pretitle ,
  (not fileReference - image)
  reference is in actions/item0/link
- ?? wknd/components/list -> core/wcm/components/list/v2/list - list of links; we don't include that for now since
  that's
  not text content belonging to the page.
- ?? cq:LiveCopy
  // titleFromAsset , descriptionFromAsset
- Test: wknd/components/text text und textIsRich
- ?? wknd/components/title : type="h3"

Alternative idea: render as HTML and convert that to markdown. Is more precise but it's difficult to avoid capturing
headers and footers and breadcrumbs and the like. -> Don't do that for now.

## OPAX

https://www.opax.ai/ ; needs configuration of Adobe Granite CSRF Filter with additional excluded path /bin/chat and
modifying Opax configuration

## Sidebar AI

/libs/wcm/core/content/editor/jcr:content/sidepanels/edit contains the code for the sidepanel.

https://github.com/Adobe-Consulting-Services/acs-aem-samples/issues/41 :
var $sidePanelEdit = $("#SidePanel").find(".js-sidePanel-edit"),
$tabs = $sidePanelEdit.data("tabs");

//add the page itree iframe in new tab
$tabs.addItem({
tabContent: "Page Browser",
panelContent: getPageContent(),
active: false
});

## References

- https://github.com/adobe/aem-project-archetype /
  https://experienceleague.adobe.com/docs/experience-manager-core-components/using/developing/archetype/using.html?lang=en /
  https://github.com/adobe/aem-project-archetype/blob/master/README.md
- https://github.com/adobe/aem-guides-wknd
- https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713
- https://experienceleague.adobe.com/docs/experience-manager-core-components/using/introduction.html?lang=en
- https://developer.adobe.com/experience-manager/reference-materials/6-5/jsdoc/ui-touch/editor-core/Granite.author.editables.html

## Multi language structure

## Icons

https://developer.adobe.com/experience-manager/reference-materials/6-5/coral-ui/coralui3/Coral.Icon.html#availableIcons
? GearsEdit , AutomatedSegment , Annotate , Scribble

For dialog calls content dialog: https://spectrum.adobe.com/page/icons/ MagicWand oder auch GearsEdit

<coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS" icon="infocircle" tabindex="0"
aria-describedby="description_bc498e27-76c6-4923-8968-8e7f8f68a3d7" alt="description" role="img"
aria-label="description" size="S">
<svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">
<use xlink:href="#spectrum-icon-24-MagicWand"></use>
</svg>
</coral-icon>

https://developer.adobe.com/experience-manager/reference-materials/6-5/coral-ui/coralui3/Coral.Icon.html#availableIcons

Possible Icons for Sidebar Dialog :
Run play , Stop stop , Reset / Clear / deleteOutline , Reset / Clear History delete, Back back , Forward forward ,
evtl fastForward

## Workarounds

Fix for archetype needed:
https://github.com/adobe/aem-project-archetype/issues/986
-D appId="composum/ai" does not work (subpackages aren't installed) -> use -D appId="composum-ai"

## Debugging

see https://experienceleague.adobe.com/docs/experience-manager-65/developing/introduction/clientlibs.html?lang=en
http://localhost:4502/libs/granite/ui/content/dumplibs.test.html?categories=cq.authoring.editor.sites.page.hook
unfortunately ?debugClientLibs=true breaks the editor. Better configure Adobe Granite HTML Library Manager

## Richtexteditor

https://www.bounteous.com/insights/2022/01/06/custom-rich-text-editor-plugins-adobe-experience-manager
https://experienceleague.adobe.com/docs/experience-manager-65/administering/operations/rich-text-editor.html?lang=en ->
clientlib rte.coralui3 , /libs/cq/gui/components/authoring/dialog/richtext/clientlibs/rte/coralui3
https://experienceleague.adobe.com/docs/experience-manager-core-components/using/wcm-components/text.html?lang=en
https://github.com/adobe/aem-core-wcm-components/tree/main/content/src/content/jcr_root/apps/core/wcm/components/text/v2/text
http://localhost:4502/bin/browser.html/libs/core/wcm/components/text/v2/text

/libs/cq/gui/components/authoring/dialog/richtext/clientlibs/rte/coralui3/js/richtext.js

!!! foundation-contentloaded
.find(".cq-RichText>.cq-RichText-editable").data('rteinstance').getContent()
Test on wcm editors, too!

http://experience-aem.blogspot.com/2016/07/aem-62-touch-ui-rich-text-editor-inplace-editing-open-in-fullscreen-editing-start-event.html
editing-start is triggered but I have trouble to catch it. TODO FIXME CONTINUE THIS

### icon

<coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS composum-ai-create-dialog-action" icon="gearsEdit" role="img" size="S">
<svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">
<use xlink:href="#spectrum-icon-18-GearsEdit"></use>
</svg>
</coral-icon>

<button is="coral-button" variant="quietaction" class="rte-toolbar-item _coral-ActionButton" type="button"
title="AI Content Creation" icon="gearsEdit" size="M"><coral-icon size="M"
class="_coral-Icon--sizeS _coral-Icon" role="img" icon="gearsEdit" alt="AI Content Creation"
aria-label="AI Content Creation">
<svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">
<use xlink:href="#spectrum-icon-18-GearsEdit"></use>
</svg>
</coral-icon><coral-button-label class="_coral-ActionButton-label"></coral-button-label></button>

## ui.frontend

run 'npm run watch' for development.

## Events

possibly relevant, but found no need yet: cq-layer-activated foundation-contentloaded

- coral-overlay:open Triggerred after the overlay is opened with show() or instance.open = true
  https://developer.adobe.com/experience-manager/reference-materials/6-5/coral-ui/coralui3/Coral.Overlay.html#Coral.Overlay:coral-overlay:open
  comes when dialog is opened, after foundation-contentloaded . coral-overlay:beforeopen doesn't work on RTE.
  Argument: dialog

- editing-start triggered in CUI.Richtext.js

### Event flow

- Page editor [1](http://localhost:4502/editor.
  html/content/wknd/language-masters/test/composum-ai-testpages.html)
  or [2](http://localhost:4502/editor.html/content/wknd/language-masters/en/faqs.html)
- [Page properties](http://localhost:4502/mnt/overlay/wcm/core/content/sites/properties.html?item=/content/wknd/language-masters/en/faqs)
- [Experience fragment](http://localhost:4502/editor.
  html/content/experience-fragments/wknd/us/en/adventures/adventures-2021/master.html)
- (not functional
  yet): [Content fragment](http://localhost:4502/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html/content/dam/wknd/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc)

1. Page editor
    - richttext in document:
        - content frame editing-start on edited div.text
    - richtext editor in dialog
        - editor frame foundation-contentloaded on coral-dialog
        - editor frame dialog-ready on document
        - editor frame editing-start on each div.cq-Richtext-editable
        - editor frame coral-overlay:open on coral-dialog

2. Page properties
    - foundation-contentloaded on document

3. Experience fragment editor
    - editor frame foundation-contentloaded and cq-layer-activated on document
    - richtext in experience fragment:
        - content frame editing-start on div.text
    - richtext in experience fragment going fullscreen
        - editor frame editing-start on div.rte-editor

4. Content fragment editor
    - foundation-content-loaded on document
    - editing-start on div.cfm-multieditor-richtext-editor

### registerdialogs.js flow

#### Registration

cq-sidepanel-loaded -> loadSidebarPanelDialog

cq-layer-activated -> initRteHooks registers registerContentDialogInRichtextEditors for editing-start on
Granite.author.ContentFrame.getDocument()

coral-overlay:open foundation-contentloaded -> prepareDialog -> (insertCreateButtonsForTextareas ,
registerContentDialogInRichtextEditors)

##### Processing

The major problem are the richtext editors: the create button has to be inserted, and the component path and resource
type, and the property name have to be found out, and the reteinstance has to be determined when the button is clicked.

Button creation is triggered on

After the editing-start the toolbar is there, but it's a bit difficult
to find it / see what it is for, as there are several places.

1.

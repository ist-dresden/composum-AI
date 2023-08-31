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

## Resource -> approximate markdown transformation

Special resource types of WKND:

- OK: wknd/components/contentfragment -> core/wcm/components/contentfragment/v1/contentfragment : fragmentPath ,
  variationName, elementNames , ??? childnodes
    - fragmentPath references /content/dam/* ; take attribute elementNames from (fragmentPath)/jcr_content/data/
      (variationName or master)
    - evtl. text attribute if present?
    - example http://localhost:4502/bin/browser.
      html/content/wknd/us/en/adventures/colorado-rock-climbing/_jcr_content/root/container/container_fixed/container/contentfragment 
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

## Workarounds

Fix for archetype needed:
https://github.com/adobe/aem-project-archetype/issues/986
-D appId="composum/ai" does not work (subpackages aren't installed) -> use -D appId="composum-ai"

## Debugging

see https://experienceleague.adobe.com/docs/experience-manager-65/developing/introduction/clientlibs.html?lang=en
http://localhost:4502/libs/granite/ui/content/dumplibs.test.html?categories=cq.authoring.editor.sites.page.hook
unfortunately ?debugClientLibs=true breaks the editor. Better configure Adobe Granite HTML Library Manager

## TODOs

- move common Java code to slingbase
-

## Misc

### ui.frontend

run 'npm run watch' for development.

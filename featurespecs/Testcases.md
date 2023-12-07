# Testcases before preparing a release

Here are some general testcases in addition to testcases for the implemented feature.
Also check for errors in the browser console during testing.

## Composum

- Sidebar AI in [page editor](http://localhost:9090/bin/pages.html/content/ist/composum/home/pages/editing/Composum-AI)
- Content creation dialog in some dialogs in page editor: text component, page properties
- page properties: keyword dialog
- switch to German, translation dialog

## AEM

Testpages:

- Page editor [1](http://localhost:4502/editor.
  html/content/wknd/language-masters/test/composum-ai-testpages.html)
  or [2](http://localhost:4502/editor.html/content/wknd/language-masters/en/faqs.html)
- [Page properties](http://localhost:4502/mnt/overlay/wcm/core/content/sites/properties.html?item=/content/wknd/language-masters/en/faqs)
- [Experience fragment](http://localhost:4502/editor.
  html/content/experience-fragments/wknd/us/en/adventures/adventures-2021/master.html)
- (not functional
  yet): [Content fragment](http://localhost:4502/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html/content/dam/wknd/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc)
  http://localhost:4502/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html/content/dam/core-components-examples/library/sample-assets/component-structured-content-fragment

Test:

- Side panel AI in page editor and experience fragment editor
- Content creation in richtext field in page / experience fragment editor
- Content creation in testcomponent dialog in page editor and page properties dialog

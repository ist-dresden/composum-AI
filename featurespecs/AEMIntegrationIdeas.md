# Ideas for AEM Integration

## Testsetup

AEM Cloud SDK with WKND Site https://wknd.site/

## Where to integrate

Text:
http://localhost:4502/editor.html/content/wknd/us/en/about-us.html
Content fragments:
http://localhost:4502/editor.html/content/dam/wknd/en/magazine/la-skateparks/ultimate-guide-to-la-skateparks
Core components:
https://experienceleague.adobe.com/docs/experience-manager-core-components/using/introduction.html?lang=en

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

## Multi language structure

## Workarounds

Fix for archetype needed:
https://github.com/adobe/aem-project-archetype/issues/986
-D appId="composum/ai" does not work (subpackages aren't installed) -> use -D appId="composum-ai" 

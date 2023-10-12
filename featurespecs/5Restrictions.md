# Restricting the AI usage

(Draft, to be implemented https://github.com/ist-dresden/composum-AI/issues/24)

For testing the AI, one could need restrictions where it is employed. That could be:
- restrictions for certain users
- restrictions to certain content paths
- restrictions to certain views, like the editor or page properties or ... , probably on the path of the URL shown in the browser
- restrictions to certain page types or components
- restrictions of the dialog (creation dialog or side panel AI)

That mechanism should go into slingbase. 

Some restrictions can be checked in the server whenever they apply to the whole page / session, but some parts could only be checked in the browser, if they apply to the various components.

We should prepare a plugin mechanism to be extensible.

Out of scope decision:
- we will not implement component specific restrictions, as that is difficult to check in the browser and likely much dependent on the system (Composum vs. AEM, AEM 6.5 vs. AEMaaCS)

Implementation decision:
- If the AI is rolled out for various sites, we need to have additive configurations. Thus restrictions have to be "enable XXX for YYY" which can be implemented by a OSGI configuration factory with suitable configurations.
- we will heavily rely on regular expressions, but allow lists of regular expressions to give a better overview. 
  There should be regular expressions for "allow" and "deny" for each category.

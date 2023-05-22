# Prompts for ChatGPT for Composum specific things

## Title attributes / i18n ( promptlib:composum.i18n )

Add title attributes for the UI elements that explain their function.
Please wrap them into the function cpn:i18n , for example instead of `title="Close"`
use `title="${cpn:i18n(slingRequest,'Close')}"` . Put those title attributes on their own line for code readability.
Apply such a wrapping to existing titles, if they are present.

Rewrite labels like `<label for="predefinedPrompts">Predefined Prompts</label>` to use the cpn:text tag:
`<cpn:text tagName="label" i18n="true">Predefined Prompts</cpn:text>`, but only if
the label does not have any subelements.

Wrap texts that are not internationalized with function cpn:i18n or the tag cpn:text in a cpn:text like this:
`Cancel` should become `<cpn:text i18n="true">Cancel</cpn:text>`.

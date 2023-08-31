package com.composum.ai.composum.bundle;

import static com.composum.ai.composum.bundle.WritePredefinedPromptsInAEMFormat.jsonListToXML;

/**
 * Writes the prompts defined in /create/contentselectors.json as Sling resources in AEM format,
 * see e.g. aem/ui.content/src/main/content/jcr_root/conf/composum-ai/settings/dialogs/contentcreation/contentselectors/.content.xml
 */
public class WriteContentSelectorsInAEMFormat {

    public static void main(String[] args) {
        final String jsonfile = "/create/contentselectors.json";
        jsonListToXML(WriteContentSelectorsInAEMFormat.class, jsonfile, true);
        System.out.println("CAUTION: the 'no content' is different (directly from source content)");
    }
}

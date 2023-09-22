package com.composum.ai.composum.bundle;

import static com.composum.ai.composum.bundle.WritePredefinedPromptsInAEMFormat.jsonListToXML;

/**
 * Writes the prompts defined in /create/textlengths.json as Sling resources in AEM format,
 * see e.g. aem/ui.content/src/main/content/jcr_root/conf/composum-ai/settings/dialogs/contentcreation/textlengths/.content.xml
 */
public class WriteTextLengthsInAEMFormat {

    public static void main(String[] args) {
        final String jsonfile = "/create/textlengths.json";
        jsonListToXML(WriteTextLengthsInAEMFormat.class, jsonfile, false);
    }
}

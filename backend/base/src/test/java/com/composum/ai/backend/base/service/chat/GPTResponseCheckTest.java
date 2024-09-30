package com.composum.ai.backend.base.service.chat;

import static com.composum.ai.backend.base.service.chat.GPTResponseCheck.KEEP_HREF_TRANSLATION_CHECK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Mostly tests for {@link GPTResponseCheck#KEEP_HREF_TRANSLATION_CHECK}.
 */
public class GPTResponseCheckTest {

    public static final String CAUTIONMSG = "CAUTION: Do not translate or change absolute or relative URLs in href attributes in HTML links, such as ";

    @Test
    public void responseProblem_noHrefsInSource_returnsNull() {
        String source = "<p>No links here</p>";
        String translation = "<p>Keine Links hier</p>";
        assertNull(KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, translation));
    }

    @Test
    public void responseProblem_matchingHrefs_returnsNull() {
        String source = "<a href=\"http://example.com\">Link</a>";
        String translation = "<a href=\"http://example.com\">Link</a>";
        assertNull(KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, translation));
    }

    @Test
    public void responseProblem_missingHrefs_returnsWarning() {
        String source = "<a href=\"http://example.com\">Link</a>";
        String translation = "<a href=\"http://beispiel.com\">Link</a>";
        String result = KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, translation);
        assertNotNull(result);
        assertTrue(result.contains(CAUTIONMSG));
        assertEquals(CAUTIONMSG + " href=\"http://example.com\" .", result);
    }

    @Test
    public void responseProblem_extraHrefsInTranslation_returnsWarning() {
        String source = "<a href=\"http://example.com\">Link</a>";
        String translation = "<a href=\"http://example.com\">Link</a><a href=\"http://extra.com\">Extra</a>";
        String result = KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, translation);
        assertNotNull(result);
        assertTrue(result.contains(CAUTIONMSG));
    }

    @Test
    public void responseProblem_longHref_ignoresHref() {
        String longlong = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        String source = "<a href=\"" + longlong + longlong + longlong + "\">Link</a>";
        String translation = "Missing but no report";
        assertNull(KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, translation));
    }

    @Test
    public void realExampleReport() {
        String source = "<h2>Strong focus on customer orientation</h2><p>We combine <a title=\"Automation, manufacturing and connection technologies\" href=\"/content/site/es/es-es/technologies.html\" target=\"_self\" rel=\"noopener noreferrer\">resources</a>, technology and <a href=\"http://www.example.net/intelligent/products/and/solutions\" target=\"_blank\" title=\"intelligent products and solutions\">people</a> to jointly create a positive change</p>";
        String correctTranslation = "<h2>Starke Fokussierung auf Kundenorientierung</h2><p>Wir verbinden <a title=\"Automations-, Fertigungs- und Verbindungstechnologien\" href=\"/content/site/es/es-es/technologies.html\" target=\"_self\" rel=\"noopener noreferrer\">Ressourcen</a>, Technologie und <a href=\"http://www.example.net/intelligent/products/and/solutions\" target=\"_blank\" title=\"intelligenter Produkte und Lösungen\">Menschen</a>, um gemeinsam einen positiven Wandel</p>";
        String result = KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, correctTranslation);
        assertNull(result);

        String brokenTranslation = "<h2>Starke Fokussierung auf Kundenorientierung</h2><p>Wir verbinden <a title=\"Automations-, Fertigungs- und Verbindungstechnologien\" href=\"/content/site/es/es-es/Technologien.html\" target=\"_self\" rel=\"noopener noreferrer\">Ressourcen</a>, Technologie und <a href=\"http://www.example.net/intelligent/produkte/und/loesungen\" target=\"_blank\" title=\"intelligenter Produkte und Lösungen\">Menschen</a>, um gemeinsam einen positiven Wandel</p>";
        result = KEEP_HREF_TRANSLATION_CHECK.responseProblem(source, brokenTranslation);
        assertEquals(CAUTIONMSG +
                " href=\"/content/site/es/es-es/technologies.html\", href=\"http://www.example.net/intelligent/products/and/solutions\" .", result);
    }

}

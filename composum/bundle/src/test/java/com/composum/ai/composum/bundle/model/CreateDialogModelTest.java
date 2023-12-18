package com.composum.ai.composum.bundle.model;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;

public class CreateDialogModelTest {

    private ApproximateMarkdownService approximateMarkdownServiceMock =
            Mockito.mock(ApproximateMarkdownService.class);

    private CreateDialogModel model = new CreateDialogModel() {{
        this.approximateMarkdownService = approximateMarkdownServiceMock;
    }};

    @Test
    public void testGetPredefinedPrompts() {
        Map<String, String> predefinedPrompts = model.getPredefinedPrompts();
        assertNotNull(predefinedPrompts);
        assertTrue(!predefinedPrompts.isEmpty());
    }

    @Test
    public void testGetContentSelectors() {
        Map<String, String> contentSelectors = model.getContentSelectors();
        assertNotNull(contentSelectors);
        assertTrue(!contentSelectors.isEmpty());
    }

    @Test
    public void testGetTextLengths() {
        Map<String, String> textLengths = model.getTextLengths();
        assertNotNull(textLengths);
        assertTrue(!textLengths.isEmpty());
    }

}

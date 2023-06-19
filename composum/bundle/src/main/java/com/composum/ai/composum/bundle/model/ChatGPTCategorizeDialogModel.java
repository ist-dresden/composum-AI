package com.composum.ai.composum.bundle.model;

import java.util.List;
import java.util.Objects;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import com.composum.ai.backend.base.service.chat.GPTContentCreationService;
import com.composum.ai.composum.bundle.ApproximateMarkdownService;
import com.composum.pages.commons.model.AbstractModel;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;

/**
 * Model for rendering the categorize dialog. It gets instantiated on a property resource, e.g. page/jcr:content/category .
 * <p>
 * The dialog is rendered with `/libs/composum/ai/pagesintegration/dialogs/categorize/categorize.jsp`
 * (resource composum/ai/pagesintegration/dialogs/categorize in Apache Sling) from
 * `com.composum.ai.composum.bundle.ChatGPTDialogServlet` and uses model
 * `model.com.composum.ai.composum.bundle.ChatGPTCategorizeDialogModel`.
 * The URL is e.g.
 * `/bin/cpm/platform/ai/dialog.categorizeDialog.html/content/ist/software/home/test/_jcr_content/category`
 * and the currently assigned categories are added as parameter 'category' (multiple values).
 * <p>
 * The suggested categories are loaded via an additional HTML AJAX request that loads the suggested categories. This is
 * implemented via a selector:
 * `/bin/cpm/platform/ai/dialog.categorizeDialog.suggestions.html/content/ist/software/home/test/_jcr_content/category`
 * `/libs/composum/ai/pagesintegration/dialogs/categorize/suggestions.jsp`
 * <p>
 * The Javascript class CategorizeDialog in `/libs/composum/ai/pagesintegration/js/chatgpt.js` triggers the loading
 * of the dialog and the AJAX call for loading the suggestions.
 * <p>
 * The current categories are not taken from the resource, but from the dialog this is called from, since the user
 * might have modified this, and are transmitted as parameter 'category' (multiple values) in the request.
 */
public class ChatGPTCategorizeDialogModel extends AbstractModel {

    /**
     * Initializes the model.
     *
     * @param context  The bean context.
     * @param resource The resource.
     */
    @Override
    public void initialize(BeanContext context, Resource resource) {
        super.initialize(context, resource);
    }

    /**
     * Returns the categories that are currently assigned. The current categories are not taken from the resource,
     * but from the dialog this is called from, since the user
     * might have modified this, and are transmitted as parameter 'category' (multiple values) in the request.
     * CAUTION: unsanitized, use cpn:text!
     */
    public List<String> getCurrentCategories() {
        SlingHttpServletRequest request = context.getRequest();
        String[] categories = request.getParameterValues("category");
        return categories != null ? List.of(categories) : List.of();
    }

    public boolean getHasCurrentCategories() {
        return !getCurrentCategories().isEmpty();
    }

    /**
     * This uses the ApproximateMarkdownService on the current page and feeds that to ChatGPT for keywording.
     */
    public List<String> getSuggestedCategories() {
        ApproximateMarkdownService markdownService = Objects.requireNonNull(context.getService(ApproximateMarkdownService.class));
        Resource pageResource = getContainingPage().getResource();
        String markdown = markdownService.approximateMarkdown(ResourceHandle.use(pageResource).getContentResource());
        GPTContentCreationService contentCreationService = Objects.requireNonNull(context.getService(GPTContentCreationService.class));
        return contentCreationService.generateKeywords(markdown);
    }
}

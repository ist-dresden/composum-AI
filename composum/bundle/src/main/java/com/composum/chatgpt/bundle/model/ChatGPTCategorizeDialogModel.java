package com.composum.chatgpt.bundle.model;

import java.util.List;
import java.util.Objects;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import com.composum.chatgpt.base.service.chat.GPTContentCreationService;
import com.composum.chatgpt.bundle.ApproximateMarkdownService;
import com.composum.pages.commons.model.AbstractModel;
import com.composum.sling.core.BeanContext;

/**
 * Model for rendering the categorize dialog. It gets instantiated on a property resource, e.g. page/jcr:content/category .
 * <p>
 * The dialog is rendered with `/libs/composum/chatgpt/pagesintegration/dialogs/categorize/categorize.jsp`
 * (resource composum/chatgpt/pagesintegration/dialogs/categorize in Apache Sling) from
 * `com.composum.chatgpt.bundle.ChatGPTDialogServlet` and uses model
 * `com.composum.chatgpt.bundle.model.ChatGPTCategorizeDialogModel`.
 * The URL is e.g.
 * `/bin/cpm/platform/chatgpt/dialog.categorizeDialog.html/content/ist/software/home/test/_jcr_content/category`
 * and the currently assigned categories are added as parameter 'category' (multiple values).
 * <p>
 * The suggested categories are loaded via an additional HTML AJAX request that loads the suggested categories. This is
 * implemented via a selector:
 * `/bin/cpm/platform/chatgpt/dialog.categorizeDialog.suggestions.html/content/ist/software/home/test/_jcr_content/category`
 * `/libs/composum/chatgpt/pagesintegration/dialogs/categorize/suggestions.jsp`
 * <p>
 * The Javascript class CategorizeDialog in `/libs/composum/chatgpt/pagesintegration/js/chatgpt.js` triggers the loading
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
        return List.of(request.getParameterValues("category"));
    }

    /**
     * This uses the ApproximateMarkdownService on the current page and feeds that to ChatGPT for keywording.
     */
    public List<String> getSuggestedCategories() {
        ApproximateMarkdownService markdownService = Objects.requireNonNull(context.getService(ApproximateMarkdownService.class));
        String markdown = markdownService.approximateMarkdown(getContainingPage().getResource());
        GPTContentCreationService contentCreationService = Objects.requireNonNull(context.getService(GPTContentCreationService.class));
        return contentCreationService.generateKeywords(markdown);
    }
}

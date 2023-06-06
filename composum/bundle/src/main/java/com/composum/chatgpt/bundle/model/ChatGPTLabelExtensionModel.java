package com.composum.chatgpt.bundle.model;

import java.util.List;

import javax.inject.Inject;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.model.Model;
import com.composum.pages.commons.taglib.EditWidgetTag;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.util.ResourceUtil;

/**
 * Businesslogic for extending the label of a widget with the various buttons.
 * At the moment we have 3 buttons: <ul>
 * <li>the translate button, only for widgettypes textfield, textarea, richtext, and only if there are texts in other languages than the current one, and if i18n="true" and multi="false"</li>
 * <li>the content creation button, visible always for widgettypes textfield, textarea, codearea, richtext, and only if multi="false"</li>
 * <li>the pagecategories button, visible only for the categories of the page, which is a multi textfield for property "category" : cpp:widget label="Category" property="category" type="textfield" multi="true"</li>
 * </ul>
 */
public class ChatGPTLabelExtensionModel extends AbstractModel {

    private static final Logger LOG = LoggerFactory.getLogger(ChatGPTLabelExtensionModel.class);

    private boolean valid;
    private EditWidgetTag widget;
    private Model model;
    private GPTChatCompletionService chatCompletionService;

    @Override
    protected void initializeWithResource(@NotNull Resource resource) {
        super.initializeWithResource(resource);
        if (context.getAttribute(EditWidgetTag.WIDGET_VAR, Object.class) instanceof EditWidgetTag && isEnabled()) {
            widget = context.getAttribute(EditWidgetTag.WIDGET_VAR, EditWidgetTag.class);
            chatCompletionService = context.getService(GPTChatCompletionService.class);
            if (widget.getModel() instanceof Model && chatCompletionService != null && chatCompletionService.isEnabled()) {
                model = (Model) widget.getModel();
                valid = true;
            }
        }
        LOG.info("initializeWithResource valid={}", valid);
    }

    public boolean isEnabled() {
        return chatCompletionService != null && chatCompletionService.isEnabled();
    }

    public String getWidgetType() {
        return widget.getWidgetType();
    }

    /**
     * If anything at all is visible. If not, we don't want to ouput any styled wrapper divs etc.
     */
    public boolean isVisible() {
        return isEnabled() && (isTranslateButtonVisible() || isContentCreationButtonVisible() || isPageCategoriesButtonVisible());
    }

    /**
     * The translation button is visible only for widgettypes textfield, textarea, richtext, and only if there are texts in other languages than the current one, and if i18n="true" and multi="false".
     */
    public boolean isTranslateButtonVisible() {
        boolean visible = valid && widget.isI18n() && !widget.isMulti();
        visible = visible && List.of("textfield", "textarea", "richtext").contains(widget.getWidgetType());
        if (visible) {
            Resource propertyResource = getResource().getChild(widget.getProperty());
            if (propertyResource == null) {
                propertyResource = new SyntheticResource(getResource().getResourceResolver(), getPath() + '/' + widget.getProperty(), "nt:unstructured");
            }
            ChatGPTTranslationDialogModel translationmodel = context.withResource(propertyResource).adaptTo(ChatGPTTranslationDialogModel.class);
            if (translationmodel != null) {
                translationmodel.setPropertyI18nPath(getPropertyI18nPath());
            }
            visible = translationmodel != null && translationmodel.isTranslationPossible();
        }
        return visible;
    }

    /**
     * The content creation button is visible always for widgettypes textfield, textarea, codearea, richtext, and only if multi="false".
     */
    public boolean isContentCreationButtonVisible() {
        boolean visible = valid && !widget.isMulti();
        visible = visible && List.of("textfield", "textarea", "codearea", "richtext").contains(widget.getWidgetType());
        visible = visible && !widget.getPropertyName().startsWith("style/category."); // not sensible for content creation.
        return visible;
    }

    /**
     * The pagecategories button is visible only for the categories of the page, which is a multi textfield for property "category" : cpp:widget label="Category" property="category" type="textfield" multi="true.
     */
    public boolean isPageCategoriesButtonVisible() {
        boolean visible = valid && widget.isMulti() && "textfield".equals(widget.getWidgetType());
        visible = visible && "category".equals(widget.getProperty());
        visible = visible && ResourceUtil.isResourceType(model.getResource(), "composum/pages/components/page");
        return visible;
    }

    /**
     * The property of the current resource which the widget edits, for instance "jcr:title", "title", "text".
     */
    public String getProperty() {
        return widget.getProperty();
    }

    /**
     * The path where the property is actually saved according to our i18n method, e.g. "i18n/de/text".
     */
    public String getPropertyI18nPath() {
        return widget.getPropertyName();
    }

    /**
     * Path to the current page.
     */
    public String getPagePath() {
        return ResourceHandle.use(getPageManager().getContainingPage(context, resource).getResource()).getContentResource().getPath();
    }
}

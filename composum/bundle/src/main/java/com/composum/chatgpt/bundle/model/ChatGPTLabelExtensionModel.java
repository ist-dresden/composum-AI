package com.composum.chatgpt.bundle.model;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.model.Model;
import com.composum.pages.commons.taglib.EditWidgetTag;
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

    @Override
    protected void initializeWithResource(@NotNull Resource resource) {
        super.initializeWithResource(resource);
        if (context.getAttribute(EditWidgetTag.WIDGET_VAR, Object.class) instanceof EditWidgetTag) {
            widget = context.getAttribute(EditWidgetTag.WIDGET_VAR, EditWidgetTag.class);
            if (widget.getModel() instanceof Model) {
                model = (Model) widget.getModel();
                valid = true;
            }
        }
        LOG.info("initializeWithResource valid={}", valid);
    }

    public String getWidgetType() {
        return widget.getWidgetType();
    }

    /**
     * If anything at all is visible. If not, we don't want to ouput any styled wrapper divs etc.
     */
    public boolean isVisible() {
        return isTranslateButtonVisible() || isContentCreationButtonVisible() || isPageCategoriesButtonVisible();
    }

    /**
     * The translation button is visible only for widgettypes textfield, textarea, richtext, and only if there are texts in other languages than the current one, and if i18n="true" and multi="false".
     */
    public boolean isTranslateButtonVisible() {
        boolean visible = valid && widget.isI18n() && !widget.isMulti();
        visible = visible && List.of("textfield", "textarea", "richtext").contains(widget.getWidgetType());
        if (visible) {
            Resource propertyResource = getResource().getChild(widget.getPropertyName());
            if (propertyResource == null) {
                propertyResource = new SyntheticResource(getResource().getResourceResolver(), getPath() + '/' + widget.getPropertyName(), "nt:unstructured");
            }
            ChatGPTTranslationDialogModel translationmodel = context.withResource(propertyResource).adaptTo(ChatGPTTranslationDialogModel.class);
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
     * The property of the current resource which the widget edits.
     */
    public String getProperty() {
        return widget.getProperty();
    }

}

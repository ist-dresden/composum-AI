package com.composum.ai.composum.bundle.model;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.SyntheticResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatCompletionService;
import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.model.Model;
import com.composum.pages.commons.model.Page;
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
public class LabelExtensionModel extends AbstractModel {

    /**
     * Additional attribute for a widget whether the ai should be visible or not.
     * E.g. <code>&lt;cpp:widget type="textfield" label="Author" name="author" value="${model.author}" aivisible="false"/></code>
     */
    public static final String ATTRIBUTE_AIVISIBLE = "aivisible";

    /**
     * For now we explicitly exclude some properties where AI is wrong, later we'll use {@link #ATTRIBUTE_AIVISIBLE},
     * but that requires changing Pages.
     */
    protected static final Set<String> IGNORED_PROPERTIES = Set.of("author", "style/category.view", "style/category.edit",
            "settings/google/api/key", "settings/here/api/key", "serviceUri", "key", "defaultValue");

    protected static final List<Pattern> IGNORED_PROPERTIES_PATTERNS = List.of(
            Pattern.compile("settings/.*/key"),
            Pattern.compile("languages/.*/(key|label|:name)"));

    private static final Logger LOG = LoggerFactory.getLogger(LabelExtensionModel.class);

    private boolean valid;
    private EditWidgetTag widget;
    private Model model;
    private GPTChatCompletionService chatCompletionService;
    private String aivisible;

    protected Boolean visibilityByKey(@Nonnull LabelExtensionVisibilityKey assistantKey) {
        Object attributeRaw = widget.getAttributeSet().get(ATTRIBUTE_AIVISIBLE);
        String attributeValue = attributeRaw != null ? String.valueOf(attributeRaw) : null;
        return LabelExtensionVisibilityKey.isVisible(attributeValue, assistantKey);
    }

    @Override
    protected void initializeWithResource(@NotNull Resource resource) {
        super.initializeWithResource(resource);
        if (context.getAttribute(EditWidgetTag.WIDGET_VAR, Object.class) instanceof EditWidgetTag) {
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

    protected boolean isIgnoredProperty() {
        String propertyName = widget.getPropertyName();
        return IGNORED_PROPERTIES.contains(propertyName) ||
                IGNORED_PROPERTIES_PATTERNS.stream().anyMatch(p -> p.matcher(propertyName).matches());
    }

    /**
     * The translation button is visible only for widgettypes textfield, textarea, richtext, and only if there are texts in other languages than the current one, and if i18n="true" and multi="false".
     */
    public boolean isTranslateButtonVisible() {
        boolean visible = valid && widget.isI18n() && !widget.isMulti();
        visible = visible && List.of("textfield", "textarea", "richtext").contains(widget.getWidgetType());
        visible = visible && !Boolean.FALSE.equals(visibilityByKey(LabelExtensionVisibilityKey.TRANSLATE));
        if (visible) {
            Resource propertyResource = getResource().getChild(widget.getProperty());
            if (propertyResource == null) {
                propertyResource = new SyntheticResource(getResource().getResourceResolver(), getPath() + '/' + widget.getProperty(), "nt:unstructured");
            }
            TranslationDialogModel translationmodel = context.withResource(propertyResource).adaptTo(TranslationDialogModel.class);
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
        Boolean visibilityByKey = visibilityByKey(LabelExtensionVisibilityKey.CREATE);
        visible = visible && !Boolean.FALSE.equals(visibilityByKey);
        visible = visible && (Boolean.TRUE.equals(visibilityByKey) || !isIgnoredProperty());
        visible = visible && List.of("textfield", "textarea", "codearea", "richtext").contains(widget.getWidgetType());
        return visible;
    }

    /**
     * The pagecategories button is visible only for the categories of the page, which is a multi textfield for property "category" : cpp:widget label="Category" property="category" type="textfield" multi="true.
     */
    public boolean isPageCategoriesButtonVisible() {
        boolean visible = valid && widget.isMulti() && "textfield".equals(widget.getWidgetType());
        visible = visible && "category".equals(widget.getProperty());
        visible = visible && !Boolean.FALSE.equals(visibilityByKey(LabelExtensionVisibilityKey.CATEGORIZE));
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
        Page containingPage = getPageManager().getContainingPage(context, resource);
        if (containingPage != null) {
            return ResourceHandle.use(containingPage.getResource()).getContentResource().getPath();
        } else { // site root
            return ResourceHandle.use(resource).getContentResource().getPath();
        }
    }


    @Override
    public String toString() {
        return "LabelExtensionModel{" +
                "resource=" + resource.getPath() +
                ", valid=" + valid +
                ", widget=" + widget +
                ", model=" + model +
                ", aivisible=" + (widget != null ? widget.getAttributeSet().get(ATTRIBUTE_AIVISIBLE) : null) +
                '}';
    }
}

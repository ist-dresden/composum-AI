package com.composum.chatgpt.bundle.model;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.Nullable;

import com.composum.pages.commons.model.AbstractModel;
import com.composum.pages.commons.taglib.PropertyEditHandle;
import com.composum.sling.core.BeanContext;

/**
 * Model for rendering the translation dialog. It gets instantiated on a property resource, e.g. page/cr:content/jcr:description .
 */
public class ChatGPTTranslationDialogModel extends AbstractModel {

    protected String propertyName;

    protected PropertyEditHandle<String> propertyEditHandle;

    @Override
    public void initialize(BeanContext context, Resource resource) {
        propertyName = resource.getName();
        super.initialize(context, resource);
        propertyEditHandle = new PropertyEditHandle(String.class);
        propertyEditHandle.setProperty(propertyName, propertyName, true);
        // FIXME(hps,04.05.23) how to determine that?
        propertyEditHandle.setMultiValue(false);
        propertyEditHandle.initialize(context, this.resource);
        // propertyEditHandle: resource e.g. /content/ist/software/home/test/jcr:content/main/text , propertyName = propertyPath = title
        // Abgeleitet e.g. TextField
        // locale=en_US, languages="en", "de", !multivalue
        // or: /content/ist/software/home/test/jcr:content , propertyName = propertyPath = jcr:title
    }

    @Nullable
    @Override
    protected Resource determineResource(@Nullable Resource initialResource) {
        if (initialResource == null || !initialResource.getValueMap().isEmpty()) {
            throw new IllegalArgumentException("Resource must be a property resource: " + initialResource);
        }
        return initialResource.getParent();
    }

    public PropertyEditHandle<String> getPropertyEditHandle() {
        return propertyEditHandle;
    }

    public String getPropertyName() {
        return propertyName;
    }
}

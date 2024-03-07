package com.composum.ai.aem.core.impl.autotranslate;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ValueMap;

class AITranslatePropertyWrapper {

    /**
     * Saves the date when a resource was automatically translated.
     * Find translated resources with /content//*[@ai_translated] .
     */
    public static final String PROPERTY_AI_TRANSLATED_DATE = "ai_translated";

    /**
     * Saves user who triggered the automatic translation of the resource.
     */
    public static final String PROPERTY_AI_TRANSLATED_BY = "ai_translatedBy";

    /**
     * Prefix for property names of saved values.
     */
    public static final String AI_PREFIX = "ai_";

    /**
     * Prefix for property names changed to language copies
     */
    public static final String LC_PREFIX = "lc_";

    /**
     * Suffix for the property name of a property that saves the original value of the property, to track when it
     * has to be re-translated.
     */
    public static final String AI_ORIGINAL_SUFFIX = "_original";

    /**
     * Suffix for the property name of a property that saves the translated value of the property, to track whether
     * it has been manually changed after automatic translation.
     */
    public static final String AI_TRANSLATED_SUFFIX = "_translated";

    /**
     * Suffix for a property name where a manual change is saved when a retranslation is done despite a manual modification.
     * Will be overwritten if another retranslation is done. Also keep original and translated values - as additional suffix.
     */
    public static final String AI_MANUAL_CHANGE_SUFFIX = "_manualChange";

    private final ModifiableValueMap targetValueMap;
    private final String propertyName;
    private final ValueMap sourceValueMap;

    public AITranslatePropertyWrapper(ValueMap sourceValueMap, ModifiableValueMap targetValueMap, String propertyName) {
        this.targetValueMap = targetValueMap;
        this.propertyName = propertyName;
        this.sourceValueMap = sourceValueMap;
    }

    public String getOriginal() {
        return sourceValueMap.get(propertyName, String.class);
    }

    public String getCurrentValue() {
        Object value = targetValueMap.get(propertyName);
        return value instanceof String ? (String) value : null;
    }

    public void setCurrentValue(String value) {
        setValue(propertyName, value);
    }

    public String getOriginalCopy() {
        return targetValueMap.get(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_ORIGINAL_SUFFIX), String.class);
    }

    public void setOriginalCopy(String value) {
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_ORIGINAL_SUFFIX), value);
    }

    public String getTranslatedCopy() {
        return targetValueMap.get(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_TRANSLATED_SUFFIX), String.class);
    }

    public void setTranslatedCopy(String value) {
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_TRANSLATED_SUFFIX), value);
    }

    private void setValue(String key, String value) {
        if (value != null) {
            targetValueMap.put(key, value);
        } else {
            targetValueMap.remove(key);
        }
    }

    public String getLcOriginal() {
        return targetValueMap.get(AutoPageTranslateServiceImpl.encodePropertyName(LC_PREFIX, propertyName, AI_ORIGINAL_SUFFIX), String.class);
    }

    public void setLcOriginal(String value) {
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(LC_PREFIX, propertyName, AI_ORIGINAL_SUFFIX), value);
    }

    public String getLcTranslated() {
        return targetValueMap.get(AutoPageTranslateServiceImpl.encodePropertyName(LC_PREFIX, propertyName, AI_TRANSLATED_SUFFIX), String.class);
    }

    public void setLcTranslated(String value) {
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(LC_PREFIX, propertyName, AI_TRANSLATED_SUFFIX), value);
    }

    public void setAiTranslatedBy(String value) {
        setValue(PROPERTY_AI_TRANSLATED_BY, value);
    }

    public void setAiTranslatedDate(Calendar value) {
        setValue(PROPERTY_AI_TRANSLATED_DATE, value != null ? value.toInstant().toString() : null);
    }

    /**
     * Currently not actually used, but save for differential retranslation.
     */
    public void saveManualChange() {
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_MANUAL_CHANGE_SUFFIX), getCurrentValue());
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_MANUAL_CHANGE_SUFFIX + AI_ORIGINAL_SUFFIX), getOriginalCopy());
        setValue(AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_MANUAL_CHANGE_SUFFIX + AI_TRANSLATED_SUFFIX), getTranslatedCopy());
    }

    public boolean hasSavedTranslation() {
        return isNotBlank(getOriginalCopy()) && isNotBlank(getTranslatedCopy());
    }

    public boolean isOriginalAsWhenLastTranslating() {
        return StringUtils.equals(getOriginal(), getOriginalCopy());
    }

    public String[] allLcKeys() {
        return new String[]{
                AutoPageTranslateServiceImpl.encodePropertyName(LC_PREFIX, propertyName, AI_ORIGINAL_SUFFIX),
                AutoPageTranslateServiceImpl.encodePropertyName(LC_PREFIX, propertyName, AI_TRANSLATED_SUFFIX)
        };
    }

    public String[] allAiKeys() {
        return new String[]{
                AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_ORIGINAL_SUFFIX),
                AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_TRANSLATED_SUFFIX),
                AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_MANUAL_CHANGE_SUFFIX),
                AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_MANUAL_CHANGE_SUFFIX + AI_ORIGINAL_SUFFIX),
                AutoPageTranslateServiceImpl.encodePropertyName(AI_PREFIX, propertyName, AI_MANUAL_CHANGE_SUFFIX + AI_TRANSLATED_SUFFIX)
        };
    }

    public String[] allGeneralKeys() {
        return new String[]{propertyName, PROPERTY_AI_TRANSLATED_BY, PROPERTY_AI_TRANSLATED_DATE};
    }

    public String[] allKeys() {
        List<String> keys = new ArrayList<>();
        Arrays.stream(allLcKeys()).map(keys::add);
        Arrays.stream(allAiKeys()).map(keys::add);
        Arrays.stream(allGeneralKeys()).map(keys::add);
        return keys.toArray(new String[0]);
    }

    /**
     * Checks whether a property was created by us and must not be translated etc.
     */
    protected static boolean isAiTranslateProperty(String name) {
        return name.startsWith(AI_PREFIX) || name.startsWith(LC_PREFIX);
    }

}

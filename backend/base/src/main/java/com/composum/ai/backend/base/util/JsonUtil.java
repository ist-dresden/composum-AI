package com.composum.ai.backend.base.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides an {@link ObjectMapper} preconfigured to match the JSON handling previously done with Gson throughout
 * this project: omits null fields on serialization, ignores unknown fields on deserialization (since the various
 * AI backends' responses contain more fields than are modeled here), and - like Gson's field-reflection based
 * default - (de)serializes fields of any visibility, not just public ones.
 */
public class JsonUtil {

    public static ObjectMapper newObjectMapper() {
        return new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

}

package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.api.resource.Resource;

import com.day.cq.wcm.api.WCMException;

/**
 * Actual logic for translating a livecopy.
 */
public interface AutoPageTranslateService {

    void translateLiveCopy(Resource resource) throws WCMException;

}

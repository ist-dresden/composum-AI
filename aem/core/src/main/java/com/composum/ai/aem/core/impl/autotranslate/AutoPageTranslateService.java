package com.composum.ai.aem.core.impl.autotranslate;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.day.cq.wcm.api.WCMException;

/**
 * Actual logic for translating a livecopy.
 */
public interface AutoPageTranslateService {

    void translateLiveCopy(Resource resource, GPTConfiguration configuration) throws WCMException, PersistenceException;

}

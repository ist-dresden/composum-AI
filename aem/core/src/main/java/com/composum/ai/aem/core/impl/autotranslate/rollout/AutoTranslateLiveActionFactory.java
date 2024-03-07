package com.composum.ai.aem.core.impl.autotranslate.rollout;

import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.aem.core.impl.autotranslate.AutoPageTranslateService;
import com.composum.ai.aem.core.impl.autotranslate.AutoTranslateService;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.msm.api.LiveActionFactory;
import com.day.cq.wcm.msm.commons.BaseActionFactory;

/**
 * Produces {@link AutoTranslateLiveActionImpl}s.
 *
 * @see "https://experienceleague.adobe.com/docs/experience-manager-65/content/sites/administering/introduction/msm-sync.html?lang=en#installed-synchronization-actions"
 * @see "https://experienceleague.adobe.com/docs/experience-manager-65/content/implementing/developing/extending-aem/extending-msm.html?lang=en#creating-a-new-synchronization-action"
 */
@Component(service = LiveActionFactory.class,
        property = {
                LiveActionFactory.LIVE_ACTION_NAME + "=" + AutoTranslateLiveActionImpl.NAME
        }
)
public class AutoTranslateLiveActionFactory extends BaseActionFactory<AutoTranslateLiveAction> {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateLiveActionFactory.class);

    @Reference
    protected AutoPageTranslateService autoPageTranslateService;

    @Reference
    protected AIConfigurationService configurationService;

    @Reference
    protected AutoTranslateService autoTranslateService;

    @Activate
    protected void activate(ComponentContext componentContext) {
        LOG.info("AutoTranslateLiveActionFactory.activate", componentContext.getProperties());
    }

    @Override
    protected AutoTranslateLiveAction newActionInstance(ValueMap valueMap) throws WCMException {
        return new AutoTranslateLiveActionImpl(valueMap, this, autoPageTranslateService, configurationService, autoTranslateService);
    }

    @Override
    public String createsAction() {
        return AutoTranslateLiveActionImpl.NAME;
    }
}

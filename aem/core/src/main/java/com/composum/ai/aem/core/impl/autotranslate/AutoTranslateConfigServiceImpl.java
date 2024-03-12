package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.Designate;

/**
 * Serves the configurations for the automatic translation service.
 */
@Component(service = AutoTranslateConfigService.class, scope = ServiceScope.SINGLETON,
// pid: backwards compatibility
        configurationPid = "com.composum.ai.aem.core.impl.autotranslate.AutoTranslateServiceImpl")
@Designate(ocd = AutoTranslateConfig.class)
public class AutoTranslateConfigServiceImpl implements AutoTranslateConfigService {

    protected List<Pattern> deniedResourceTypes = new ArrayList<>();

    private AutoTranslateConfig config;

    @Activate
    @Modified
    public void activate(AutoTranslateConfig config) {
        this.config = config;
        deniedResourceTypes.clear();
        for (final String rule : config.deniedResourceTypes()) {
            if (StringUtils.isNotBlank(rule)) {
                deniedResourceTypes.add(Pattern.compile(rule));
            }
        }
    }

    @Deactivate
    public void deactivate() {
        this.config = null;
    }

    @Override
    public boolean isPocUiEnabled() {
        return config != null && config.pocUiEnabled();
    }

    @Override
    public boolean isEnabled() {
        return config != null && !config.disabled();
    }

    @Override
    public boolean isTranslatableResource(@Nonnull final Resource resource) {
        if (!isEnabled()) {
            return false;
        }
        final String resourceType = resource.getResourceType();
        for (final Pattern pattern : deniedResourceTypes) {
            if (pattern.matcher(resourceType).matches()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public List<String> translateableAttributes(@Nullable Resource resource) {
        // FIXME(hps,12.03.24) implement this
        throw new UnsupportedOperationException("Not implemented yet: AutoTranslateConfigServiceImpl.translateableAttributes");
    }

}

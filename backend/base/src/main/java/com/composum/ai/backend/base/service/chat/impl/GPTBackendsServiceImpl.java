package com.composum.ai.backend.base.service.chat.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTBackendConfiguration;
import com.composum.ai.backend.base.service.chat.GPTBackendsConfigurationService;
import com.composum.ai.backend.base.service.chat.GPTBackendsService;

@Component
public class GPTBackendsServiceImpl implements GPTBackendsService {

    private static final Logger LOG = LoggerFactory.getLogger(GPTBackendsServiceImpl.class);

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected List<GPTBackendsConfigurationService> backendsConfigurationServices;

    @Nonnull
    @Override
    public List<String> getAllModels() {
        List<String> allModels = new ArrayList<>();
        if (backendsConfigurationServices != null) {
            for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
                for (GPTBackendConfiguration backend : service.getBackends()) {
                    if (!backend.disabled()) {
                        allModels.addAll(service.getModelsForBackend(backend.backendId()));
                    }
                }
            }
        }
        return allModels;
    }

    @Nonnull
    @Override
    public List<String> getActiveBackends() {
        List<String> activeBackends = new ArrayList<>();
        if (backendsConfigurationServices != null) {
            for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
                for (GPTBackendConfiguration backend : service.getBackends()) {
                    if (!backend.disabled()) {
                        activeBackends.add(backend.backendId());
                    }
                }
            }
        }
        return activeBackends;
    }

    @Nullable
    @Override
    public String getModelNameInBackend(@Nullable String model) {
        if (model == null || model.trim().isEmpty()) {
            return null;
        }
        if (model.contains(":")) {
            return model.substring(model.indexOf(':') + 1).trim();
        }
        return model.trim();
    }

    @Nullable
    protected String getBackendNameFromPrefixedModel(@Nullable String model) {
        if (model == null || model.trim().isEmpty()) {
            return null;
        }
        if (model.contains(":")) {
            return model.substring(0, model.indexOf(':')).trim();
        }
        return null;
    }

    @Override
    public GPTBackendConfiguration getConfigurationForModel(@Nonnull String model) {
        String backendName = getBackendNameFromPrefixedModel(model);
        String modelname = getModelNameInBackend(model);
        if (backendsConfigurationServices == null || modelname == null) {
            return null;
        }
        GPTBackendConfiguration conf = null;
        for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
            for (GPTBackendConfiguration backend : service.getBackends()) {
                if (Objects.equals(backend.backendId(), backendName) || service.getModelsForBackend(backend.backendId()).contains(model)) {
                    conf = backend;
                    break;
                }
            }
        }
        if (conf != null && conf.disabled()) {
            LOG.warn("Requesting access to disabled backend {} by model {}", conf.backendId(), model);
        }
        return conf;
    }

}

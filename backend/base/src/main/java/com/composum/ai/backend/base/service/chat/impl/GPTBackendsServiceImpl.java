package com.composum.ai.backend.base.service.chat.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.composum.ai.backend.base.service.chat.GPTBackendConfiguration;
import com.composum.ai.backend.base.service.chat.GPTBackendsConfigurationService;
import com.composum.ai.backend.base.service.chat.GPTBackendsService;

@Component
public class GPTBackendsServiceImpl implements GPTBackendsService {

    @Reference(cardinality = ReferenceCardinality.MULTIPLE)
    protected List<GPTBackendsConfigurationService> backendsConfigurationServices;

    @Nonnull
    @Override
    public List<String> getAllModels() {
        List<String> allModels = new ArrayList<>();
        if (backendsConfigurationServices != null) {
            for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
                for (GPTBackendConfiguration backend : service.getBackends()) {
                    allModels.addAll(service.getModelsForBackend(backend.backendId()));
                }
            }
        }
        return allModels;
    }

    @Nullable
    @Override
    public String getModelName(@Nullable String model) {
        if (model == null || model.trim().isEmpty()) {
            return null;
        }
        if (model.contains(":")) {
            return model.substring(model.indexOf(':') + 1).trim();
        }
        return model.trim();
    }

    @Override
    public GPTBackendConfiguration getConfigurationForModel(@Nonnull String model) {
        String modelname = getModelName(model);
        if (backendsConfigurationServices == null || modelname == null) {
            return null;
        }
        for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
            for (GPTBackendConfiguration backend : service.getBackends()) {
                if (service.getModelsForBackend(backend.backendId()).contains(model)) {
                    return backend;
                }
            }
        }
        return null;
    }

}

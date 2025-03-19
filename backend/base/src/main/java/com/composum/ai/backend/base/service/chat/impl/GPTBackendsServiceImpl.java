package com.composum.ai.backend.base.service.chat.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.composum.ai.backend.base.service.chat.RateLimiter;
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
        if (backendsConfigurationServices == null) {
            return new ArrayList<>();
        }
        List<String> allModels = new ArrayList<>();
        for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
            for (GPTBackendConfiguration backend : service.getBackends()) {
                allModels.addAll(service.getModelsForBackend(backend.backendId()));
            }
        }
        return allModels;
    }

    @Override
    public GPTBackendConfiguration getConfigurationForModel(@Nonnull String model) {
        if (backendsConfigurationServices == null) {
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

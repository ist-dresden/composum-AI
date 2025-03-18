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

    protected Map<String, RateLimiter> rateLimiters = Collections.synchronizedMap(new java.util.HashMap<>());

    @Nonnull
    @Override
    public List<String> getAllModels() {
        if (backendsConfigurationServices == null) {
            return new ArrayList<>();
        }
        List<String> allModels = new ArrayList<>();
        for (GPTBackendsConfigurationService service : backendsConfigurationServices) {
            allModels.addAll(service.getBackends().stream()
                    .flatMap(backend -> service.getModelsForBackend(backend.backendId()).stream())
                    .collect(Collectors.toList()));
        }
        return allModels;
    }

    @Override
    public GPTBackendConfiguration getConfiguration(@Nonnull String model) {
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


    @Nonnull
    @Override
    public RateLimiter getRateLimiter(@Nonnull String backendId) {
        return rateLimiters.computeIfAbsent(backendId, id -> {
            GPTBackendConfiguration config = getConfiguration(id);
            if (config == null) {
                throw new IllegalArgumentException("But: no configuration found for backendId: " + backendId);
            }

            int limitPerDay = config.requestsPerDay() > 0 ? config.requestsPerDay() : 3000;
            RateLimiter dayLimiter = new RateLimiter(null, limitPerDay, 1, TimeUnit.DAYS);

            int limitPerHour = config.requestsPerHour() > 0 ? config.requestsPerHour() : 1000;
            RateLimiter hourLimiter = new RateLimiter(dayLimiter, limitPerHour, 1, TimeUnit.HOURS);

            int limitPerMinute = config.requestsPerMinute() > 0 ? config.requestsPerMinute() : 100;
            return new RateLimiter(hourLimiter, limitPerMinute, 1, TimeUnit.MINUTES);
        });
    }
}

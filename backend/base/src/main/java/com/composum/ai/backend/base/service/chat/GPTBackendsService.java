package com.composum.ai.backend.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A service managing the backends for {@link GPTChatCompletionService} etc.
 */
public interface GPTBackendsService {

    /**
     * List of all model names configured for the active backends, no matter from which backend.
     */
    @Nonnull
    List<String> getAllModels();

    /**
     * Returns a list of active backends. Those can be used as prefix for unconfigured models <code>backendId:modelname</code>.
     */
    @Nonnull
    List<String> getActiveBackends();

    /**
     * Determines the backend a model is from.
     *
     * @param model the model name , possibly in the form <code>backendId:modelname</code> - in that case it can be a model that is not configured explicitly in the backend.
     * @return the backend configuration for the model or null if no backend is found.
     */
    @Nullable
    GPTBackendConfiguration getConfigurationForModel(@Nonnull String model);

    /**
     * If the model is of the form <code>backendId:modelname</code> then this returns the modelname, otherwise returns the model as it is.
     */
    @Nullable
    String getModelNameInBackend(@Nullable String model);

}

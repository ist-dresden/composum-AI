package com.composum.ai.backend.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;

/** A service that lists all backends usable with {@link GPTChatCompletionService} etc. */
public interface GPTBackendsConfigurationService {

    @Nonnull
    List<GPTBackendConfiguration> getBackends();

    @Nonnull
    List<String> getModelsForBackend(@Nonnull String backendId);

}

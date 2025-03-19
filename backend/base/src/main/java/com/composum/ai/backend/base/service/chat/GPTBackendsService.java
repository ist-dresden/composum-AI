package com.composum.ai.backend.base.service.chat;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A service managing the backends for {@link GPTChatCompletionService} etc.
 */
public interface GPTBackendsService {

    @Nonnull
    List<String> getAllModels();

    @Nullable
    GPTBackendConfiguration getConfiguration(@Nonnull String model);

}

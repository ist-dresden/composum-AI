package com.composum.ai.backend.slingbase.experimential;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

/**
 * Experimential service to provide page templating functionality: a page / component contains various prompts in it's properties
 * which are replaced by the response of the AI.
 * Status: experimential: don't use this in production yet, changes are very likely.
 *
 * @see "10PageTemplating.md"
 */
public interface AITemplatingService {

    /**
     * Replaces all prompts in the resource with the response of the AI.
     *
     * @param resource         the resource to replace the prompts in
     * @param additionalPrompt optionally, an additional prompt to add to the AI request
     * @return true if the resource was changed, false if it was not changed; no commit yet.
     */
    boolean replacePromptsInResource(Resource resource, String additionalPrompt, List<URI> additionalUrls) throws URISyntaxException, IOException;

    /**
     * Undo #replacePromptsInResource. Not commited yet.
     * @return true if the resource was changed, false if it was not changed.
     */
    boolean resetToPrompts(Resource resource) throws PersistenceException;

}

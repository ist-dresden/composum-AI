package com.composum.ai.backend.slingbase;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

/**
 * Basic services for retrieval augmented generation (RAG).
 */
public interface RAGService {

    /**
     * Returns a list of up to limit paths that might be related to the query. We search for the whole query, andy quoted strings inside and
     * then for all single words in the query, scored. The paths are returned in descending order of score.
     *
     * @param root      the root resource to search in
     * @param querytext the query text
     * @param limit     the maximum number of paths to return
     * @return a list of paths to jcr:content nodes, empty if no results or any of the parameters don't fit.
     */
    @Nonnull
    List<String> searchRelated(@Nullable Resource root, @Nullable String querytext, int limit) throws RepositoryException;

    /**
     * Finds the resources whose markdown approximation has embeddings that are the most similar to the querytext embedding.
     * Useable e.g. as filter after {@link #searchRelated(Resource, String, int)}.
     *
     * @param querytext    the query text
     * @param resources    the list of resources to search in
     * @param request      the request to use when determining the markdown approximation - not modified
     * @param response     the response to use when determining the markdown approximation - not modified
     * @param rootResource the root resource to search in
     */
    @Nonnull
    List<Resource> orderByEmbedding(@Nullable String querytext, @Nonnull List<Resource> resources,
                                    @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response,
                                    @Nonnull Resource rootResource) throws RepositoryException;

    /**
     * Answer a question with RAG from the given resources, e.g. found with {@link #searchRelated(Resource, String, int)}.
     *
     * @param querytext the query text
     * @param resources the list of resources to answer from
     * @param request   the request to use when determining the markdown approximation - not modified
     * @param response  the response to use when determining the markdown approximation - not modified
     * @param rootResource the root resource to find GPT configuration from
     * @param limitRagTexts the maximum number of RAG texts to consider
     * @return the answer text
     */
    @Nonnull
    String ragAnswer(@Nullable String querytext, @Nonnull List<Resource> resources,
                     @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response,
                     @NotNull Resource rootResource, int limitRagTexts);

}

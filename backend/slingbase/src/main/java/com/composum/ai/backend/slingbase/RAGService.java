package com.composum.ai.backend.slingbase;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;

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

}

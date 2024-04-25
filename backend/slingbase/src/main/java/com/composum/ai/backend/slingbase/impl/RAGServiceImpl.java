package com.composum.ai.backend.slingbase.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.RAGService;

/**
 * Basic services for retrieval augmented generation (RAG).
 */
@Component(service = RAGService.class)
public class RAGServiceImpl implements RAGService {

    private static final Logger LOG = LoggerFactory.getLogger(RAGServiceImpl.class);

    @Reference
    protected ApproximateMarkdownService markdownService;

    @Override
    @Nonnull
    public List<String> searchRelated(@Nullable Resource root, @Nullable String querytext, int limit) throws RepositoryException {
        if (root == null || querytext == null || limit <= 0) {
            return Collections.emptyList();
        }
        int restOfLimit = limit * 5 / 4 + 3; // a little larger since there might be exact and inexact matches

        String exactQuery = "\"" + querytext.replaceAll("\"", "") + "\"";
        String normalizedQuery = normalize(querytext);

        @NotNull List<String> exactResult = Collections.emptyList();
        try {
            exactResult = containsQuery(root, exactQuery, restOfLimit);
        } catch (RepositoryException e) {
            LOG.error("Error searching for exact query {}", exactQuery, e);
        }
        restOfLimit -= exactResult.size();

        @NotNull List<String> normalizedResult = Collections.emptyList();
        try {
            containsQuery(root, normalizedQuery, restOfLimit);
        } catch (RepositoryException e) {
            LOG.error("Error searching for normalized query {}", normalizedQuery, e);
        }

        List<String> result = new ArrayList<>(exactResult);
        result.addAll(normalizedResult);
        result = result.stream().distinct().limit(limit).collect(Collectors.toList());
        return result;
    }

    protected @NotNull List<String> containsQuery(@NotNull Resource root, @NotNull String querytext, int restOfLimit) throws RepositoryException {
        List<String> result = new ArrayList<>();
        ResourceResolver resolver = root.getResourceResolver();
        final Session session = resolver.adaptTo(Session.class);
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        String statement = "SELECT [jcr:path], [rep:score()] FROM [nt:base] AS content WHERE " +
                "ISDESCENDANTNODE(content, '" + root.getPath() + "') " +
                "AND NAME(content) = 'jcr:content' " +
                "AND CONTAINS(content.*, $queryText) " +
                "ORDER BY [rep:score] DESC";
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);
        query.bindValue("queryText", session.getValueFactory().createValue(querytext));
        query.setLimit(restOfLimit);
        LOG.debug("Executing query: {}", query.getStatement());
        QueryResult queryResult = query.execute();
        for (RowIterator rowIterator = queryResult.getRows(); rowIterator.hasNext(); ) {
            if (restOfLimit-- <= 0) {
                return result;
            }
            Row row = rowIterator.nextRow();
            LOG.debug("Found path {} with score {}", row.getPath(), row.getValue("rep:score()"));
            if (!result.contains(row.getPath())) {
                result.add(row.getPath());
            }
        }
        return result;
    }

    /**
     * Turn it into a query for the words mentioned in there - that is, remove all meta characters for CONTAINS queries:
     * AND, OR, words prefixed with -, quotes, backslashes
     */
    @Nonnull
    protected String normalize(@Nonnull String querytext) {
        return querytext
                .replaceAll("[\"\\\\]", " ")
                // words "AND", "OR" (surrounded by word boundaries)
                .replaceAll("\\b(AND|OR)\\b", " ")
                // prefixes - of words
                .replaceAll("(^|\\W)-+", "$1")
                .replaceAll("\\s+", " ")
                .trim();
    }

}

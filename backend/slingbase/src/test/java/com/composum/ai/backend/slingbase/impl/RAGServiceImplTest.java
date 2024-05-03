package com.composum.ai.backend.slingbase.impl;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class RAGServiceImplTest {

    @Rule
    public SlingContext context =
            new SlingContextBuilder(ResourceResolverType.JCR_OAK).plugin(CACONFIG).build();

    @Rule
    public ErrorCollector collector = new ErrorCollector();


    private RAGServiceImpl service = new RAGServiceImpl();

    private String rootPath;

    private Resource rootPageContentResource;

    @Before
    public void setupRootNode() throws PersistenceException {
        rootPath = context.uniqueRoot().content() + "/foo/bar";
        rootPageContentResource = context.create().resource(rootPath + "/jcr:content", "sling:vanityPath", "renditions are build on demand");
        context.resourceResolver().commit();
    }

    // @Before
    // That doesn't work. :-( Keep it for now because there might just be some dependencies needed
    public void setUp() throws PersistenceException {
        context.load().json("/lucene/contentLuceneIndex-onlytests.json", "/oak:index/contentTextLucene");
        context.resourceResolver().commit();
        assertThat(context.resourceResolver().getResource("/oak:index/contentTextLucene"), notNullValue());
        assertThat(context.resourceResolver().getResource("/oak:index/contentTextLucene/aggregates"), notNullValue());
        context.resourceResolver().getResource("/oak:index/contentTextLucene").adaptTo(ModifiableValueMap.class).put("reindex", true);
        context.resourceResolver().commit();
    }

    @Test
    @Ignore("CONTAINS doesn't seem to work here.")
    public void returnsAPathWhenQueryIsSuccessful() throws Exception {
        context.create().resource(rootPageContentResource.getPath() + "/subnode", "text", "renditions are build on demand");
        context.resourceResolver().commit();
        List<String> result = service.searchRelated(rootPageContentResource.getParent(), "renditions are build on demand", 10);
        assertThat(result.toString(), result.size(), is(1));
        assertThat(result.get(0), is(rootPath + "/jcr:content"));
    }

    /**
     * Since CONTAINS doesn't work, we at least check that there are no exceptions, even if the result is empty.
     */
    @Test
    public void queryDoesNotThrow() throws Exception {
        for (String query : new String[]{"notthere", "+something", "\"that is not found", "+-+xxx", "\"bla\"", "foo AND", "(", "foo -"}) {
            List<String> result = service.searchRelated(rootPageContentResource.getParent(), query, 10);
            assertThat(result.size(), is(0));
        }
    }

    @Test
    public void returnsEmptyListWhenRootIsNull() throws Exception {
        List<String> result = service.searchRelated(null, "query", 10);
        assertThat(result.size(), is(0));
    }

    @Test
    public void returnsEmptyListWhenQueryTextIsNull() throws Exception {
        List<String> result = service.searchRelated(rootPageContentResource.getParent(), null, 10);
        assertThat(result.size(), is(0));
    }

    @Test
    public void returnsEmptyListWhenLimitIsZero() throws Exception {
        List<String> result = service.searchRelated(rootPageContentResource.getParent(), "query", 0);
        assertThat(result.size(), is(0));
    }


    @Test
    public void normalizeHandlesVariousScenarios() {
        RAGServiceImpl service = new RAGServiceImpl();
        collector.checkThat(service.normalize("\"AND\" OR -NOT"), is("NOT"));
        collector.checkThat(service.normalize("Hello-World"), is("Hello-World"));
        collector.checkThat(service.normalize("-Hello World"), is("Hello OR World"));
        collector.checkThat(service.normalize("\"Hello World\""), is("Hello OR World"));
        collector.checkThat(service.normalize("Hello\\World"), is("HelloWorld"));
        collector.checkThat(service.normalize("Hello  World"), is("Hello OR World"));
        collector.checkThat(service.normalize("Hello AND World"), is("Hello OR World"));
        collector.checkThat(service.normalize("Hello OR World"), is("Hello OR World"));
    }

}

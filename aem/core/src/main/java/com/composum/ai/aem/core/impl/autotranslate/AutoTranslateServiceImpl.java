package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.slingbase.AIConfigurationService;
import com.day.cq.wcm.api.WCMException;

/**
 * A service that provides automatic translation of AEM pages.
 * <p>
 * This is a proof-of-concept implementation, only available if explicitly enabled in the OSGi configuration.
 */
@Designate(ocd = AutoTranslateServiceImpl.Config.class)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, scope = ServiceScope.SINGLETON)
// REQUIRE since this is currently only a POC
public class AutoTranslateServiceImpl implements AutoTranslateService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateServiceImpl.class);

    @Reference
    private ThreadPoolManager threadPoolManager;

    @Reference
    private AutoTranslateStateService stateService;

    @Reference
    private AutoPageTranslateService pageTranslateService;

    @Reference
    private AIConfigurationService configurationService;

    private ThreadPool threadPool;

    private boolean disabled;

    @Override
    public List<TranslationRun> getTranslationRuns() {
        List<TranslationRun> runs = new ArrayList<>();
        runs.addAll(stateService.getTranslationRuns());
        Collections.reverse(runs);
        return runs;
    }

    @Activate
    public void activate(Config config) {
        disabled = config.disabled();
        if (!config.disabled()) {
            threadPool = threadPoolManager.get(getClass().getName());
        }
    }

    @Deactivate
    public void deactivate() {
        disabled = true;
        if (threadPool != null) {
            threadPoolManager.release(threadPool);
        }
    }

    protected ThreadPool getThreadPool() {
        if (threadPool == null) {
            threadPool = threadPoolManager.get(getClass().getName());
        }
        return threadPool;
    }

    @Override
    public boolean isEnabled() {
        return !disabled;
    }

    @Override
    public TranslationRun startTranslation(
            @Nonnull ResourceResolver resourceResolver, @Nonnull String path,
            @Nonnull TranslationParameters translationParameters, @Nullable GPTConfiguration configuration)
            throws LoginException, PersistenceException {
        if (disabled) {
            throw new IllegalStateException("AutoTranslateService is disabled");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }
        if (!path.startsWith("/content/")) {
            throw new IllegalArgumentException("Only pages below /content/ are supported: " + path);
        }

        if (stateService.getTranslationRuns().stream()
                .filter(r -> r.rootPath.equals(path) && r.stopTime == null)
                .findAny().orElse(null) != null) {
            throw new IllegalArgumentException("Translation run for " + path + " is already running");
        }

        ResourceResolver processResolver = resourceResolver.clone(null);
        Resource root = processResolver.getResource(path);
        if (root == null) {
            throw new IllegalArgumentException("No such resource: " + path);
        }
        List<Resource> resources;
        if (translationParameters.recursive) {
            resources = collectPages(root);
        } else {
            if (root.isResourceType("cq:Page")) {
                resources = Collections.singletonList(root.getChild("jcr:content"));
            } else if (!root.getPath().contains("/jcr:content")) {
                throw new IllegalArgumentException("Not a page or a resource in a page: " + path);
            } else {
                resources = Collections.singletonList(root);
            }
        }

        TranslationRunImpl run = new TranslationRunImpl();
        run.id = "" + Math.abs(System.nanoTime());
        run.rootPath = path;
        run.translationParameters = translationParameters;
        run.translatedPages = resources.stream()
                .map(r -> new TranslationPageImpl(r.getPath()))
                .collect(Collectors.toList());
        stateService.getTranslationRuns().add(run);
        run.future = getThreadPool().submit(() -> run.execute(processResolver));
        run.status = "scheduled";
        run.user = resourceResolver.getUserID();
        run.configuration = configuration;
        return run;
    }

    protected List<Resource> collectPages(Resource root) {
        if (root.getPath().contains("/jcr:content")) {
            return Collections.singletonList(root);
        }
        // find all jcr:content nodes below root and return a list of these
        List<Resource> pages = new ArrayList<>();
        if (root.isResourceType("cq:PageContent") || root.isResourceType("dam:AssetContent")) {
            pages.add(root);
        } else {
            root.getChildren().forEach(child -> pages.addAll(collectPages(child)));
        }
        return pages;
    }

    @Override
    public void rollback(Resource resource) throws WCMException {
        pageTranslateService.rollback(resource);
    }

    protected void doRollback(ResourceResolver resourceResolver, TranslationRunImpl translationRun) throws PersistenceException, WCMException {
        for (TranslationPageImpl page : translationRun.translatedPages) {
            Resource resource = resourceResolver.getResource(page.resourcePath);
            if (resource != null) {
                pageTranslateService.rollback(resource);
            }
            resourceResolver.commit();
        }
    }

    public class TranslationRunImpl extends TranslationRun {
        public Future<?> future;
        List<TranslationPageImpl> translatedPages;
        GPTConfiguration configuration;
        TranslationParameters translationParameters;

        @Override
        public List<TranslationPage> getTranslatedPages() {
            return Collections.unmodifiableList(translatedPages);
        }

        public void cancel() {
            if (future != null) {
                future.cancel(true);
                status = "cancelling";
            }
        }

        @Override
        public synchronized void rollback(@Nonnull ResourceResolver resourceResolver) throws PersistenceException, WCMException {
            try {
                AutoTranslateServiceImpl.this.doRollback(resourceResolver, this);
            } catch (PersistenceException | WCMException | RuntimeException e) {
                messages.append("Error rolling back: " + e.toString() + "\n");
                throw e;
            }
        }

        /**
         * Translate the pages; close the resolver when done.
         */
        public void execute(ResourceResolver callResourceResolver) {
            try {
                Thread.sleep(2000); // delay a little since that is used during creating a livecopy, and that should be finished.
                status = "running";
                boolean hasErrors = false;
                startTime = new Date().toString();
                boolean interrupted = false;
                GPTConfiguration mergedConfiguration = configuration;
                if (translationParameters.additionalInstructions != null ||
                        !translationParameters.additionalInstructions.trim().isEmpty()) {
                    mergedConfiguration = new GPTConfiguration(null, null, null,
                            translationParameters.additionalInstructions).merge(configuration);
                }
                for (TranslationPageImpl page : translatedPages) {
                    interrupted = interrupted || future == null || future.isCancelled();
                    if (!interrupted && Thread.interrupted()) {
                        Thread.currentThread().interrupt();
                        interrupted = true;
                    }
                    if (interrupted) {
                        status = "cancelled";
                        page.status = "cancelled";
                        continue;
                    }

                    page.status = "running";
                    ResourceResolver resourceResolver = null;
                    try {
                        resourceResolver = callResourceResolver.clone(null);
                        resourceResolver.revert();
                        resourceResolver.refresh();
                        Resource resource = resourceResolver.getResource(page.resourcePath);
                        AutoPageTranslateService.Stats stats = pageTranslateService.translateLiveCopy(resource,
                                mergedConfiguration, translationParameters);
                        page.stats = stats;
                        page.status = stats.hasChanges() ? "done" : "unchanged";
                    } catch (Exception e) {
                        page.status = "error";
                        this.messages.append("Error translating " + page.pagePath + ": " + e.toString() + "\n");
                        hasErrors = true;
                        LOG.error("Error translating " + page.pagePath, e);
                    } finally {
                        if (resourceResolver != null) {
                            resourceResolver.close();
                        }
                    }
                }
                status = hasErrors ? "doneWithErrors" : interrupted ? "cancelled" : "done";
                stopTime = new Date().toString();
            } catch (InterruptedException e) {
                LOG.error("" + e, e);
                Thread.currentThread().interrupt();
            } finally {
                future = null;
                callResourceResolver.close();
            }
        }
    }

    public static class TranslationPageImpl extends TranslationPage {
        String resourcePath;

        public TranslationPageImpl(String resourcePath) {
            this.resourcePath = resourcePath;
            pagePath = ResourceUtil.getParent(resourcePath); // remove jcr:content
        }
    }

    @ObjectClassDefinition(name = "Composum AI Autotranslate Configuration",
            description = "Automatic translation of AEM pages at /apps/composum-ai/components/autotranslate/list/list.html ." +
                    "Proof of concept quality - might work, but use at your own peril. :-)")
    public @interface Config {

        @AttributeDefinition(name = "Disable the Autotranslate service", defaultValue = "true")
        boolean disabled() default true;

    }

}

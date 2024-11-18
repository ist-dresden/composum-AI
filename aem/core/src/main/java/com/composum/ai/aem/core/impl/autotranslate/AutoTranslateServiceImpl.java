package com.composum.ai.aem.core.impl.autotranslate;

import static com.composum.ai.aem.core.impl.autotranslate.AITranslatePropertyWrapper.AI_TRANSLATION_ERRORMARKER;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.GPTException;
import com.day.cq.wcm.api.WCMException;

/**
 * A service that provides automatic translation of AEM pages.
 * The actual work is done in {@link AutoPageTranslateServiceImpl};
 * this is for managing the translation queue and the thread pool.
 * <p>
 * This is a proof-of-concept implementation, only available if explicitly enabled in the OSGi configuration.
 */
@Component
public class AutoTranslateServiceImpl implements AutoTranslateService {

    private static final Logger LOG = LoggerFactory.getLogger(AutoTranslateServiceImpl.class);

    @Reference
    private ThreadPoolManager threadPoolManager;

    @Reference
    private AutoTranslateStateService stateService;

    @Reference
    private AutoPageTranslateService pageTranslateService;

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected volatile AutoTranslateConfigService autoTranslateConfigService;

    private ThreadPool threadPool;

    @Override
    public List<TranslationRun> getTranslationRuns() {
        List<TranslationRun> runs = new ArrayList<>(stateService.getTranslationRuns());
        Collections.reverse(runs);
        return runs;
    }

    @Activate
    @Modified
    protected void activate() {
        if (isEnabled() && threadPool == null) {
            threadPool = threadPoolManager.get(getClass().getName());
        }
    }

    @Deactivate
    public void deactivate() {
        if (threadPool != null) {
            threadPoolManager.release(threadPool);
        }
    }

    @Override
    public boolean isEnabled() {
        return autoTranslateConfigService.isEnabled();
    }

    protected ThreadPool getThreadPool() {
        if (threadPool == null) {
            threadPool = threadPoolManager.get(getClass().getName());
        }
        return threadPool;
    }

    @Override
    public TranslationRun startTranslation(
            @Nonnull ResourceResolver resourceResolver, @Nonnull String path,
            @Nonnull TranslationParameters translationParameters)
            throws LoginException {
        if (!isEnabled()) {
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
            int maxDepth = translationParameters.maxDepth != null ? translationParameters.maxDepth : Integer.MAX_VALUE;
            resources = collectPages(root, maxDepth);
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
        run.translationParameters = translationParameters.clone();
        run.translationParameters.autoSave = true; // otherwise it'll be just rolled back
        run.translatedPages = resources.stream()
                .map(r -> new TranslationPageImpl(r))
                .collect(Collectors.toList());
        run.waituntil = System.currentTimeMillis() + 1000; // when triggered during live copy creation.
        run.status = TranslationStatus.QUEUED;
        run.user = resourceResolver.getUserID();
        stateService.getTranslationRuns().add(run);
        run.future = getThreadPool().submit(() -> run.execute(processResolver));
        return run;
    }

    protected List<Resource> collectPages(Resource root, int maxDepth) {
        if (maxDepth < 0 || root.getName().endsWith(AutoTranslateListModel.SUFFIX_TRANSLATECOPY)) {
            return Collections.emptyList();
        }
        if (root.getPath().contains("/jcr:content")) {
            return Collections.singletonList(root);
        }
        // find all jcr:content nodes below root and return a list of these
        List<Resource> pages = new ArrayList<>();
        if (root.isResourceType("cq:PageContent") || root.isResourceType("dam:AssetContent")) {
            pages.add(root);
        } else {
            root.getChildren().forEach(child -> pages.addAll(collectPages(child, maxDepth - 1)));
        }
        return pages;
    }

    @Override
    public void rollback(Resource resource) throws WCMException, PersistenceException {
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
        public long waituntil;
        List<TranslationPageImpl> translatedPages;
        TranslationParameters translationParameters;

        @Override
        public List<TranslationPage> getTranslatedPages() {
            return Collections.unmodifiableList(translatedPages);
        }

        public void cancel() {
            if (future != null) {
                future.cancel(true);
                status = TranslationStatus.CANCELLING;
            }
        }

        @Override
        public synchronized void rollback(@Nonnull ResourceResolver resourceResolver) throws PersistenceException, WCMException {
            try {
                AutoTranslateServiceImpl.this.doRollback(resourceResolver, this);
            } catch (PersistenceException | WCMException | RuntimeException e) {
                messages.append("Error rolling back: ").append(e).append("\n");
                throw e;
            }
        }

        /**
         * Translate the pages; close the resolver when done.
         */
        public void execute(ResourceResolver callResourceResolver) {
            try {
                status = TranslationStatus.RUNNING;
                if (System.currentTimeMillis() < waituntil) {
                    // delay a little since that is used during creating a livecopy, and that should be finished.
                    Thread.sleep(waituntil - System.currentTimeMillis());
                }
                boolean hasErrors = false;
                startTime = new Date().toString();
                boolean interrupted = false;
                for (TranslationPageImpl page : translatedPages) {
                    interrupted = interrupted || future == null || future.isCancelled();
                    if (!interrupted && Thread.interrupted()) {
                        Thread.currentThread().interrupt();
                        interrupted = true;
                    }
                    if (interrupted) {
                        status = TranslationStatus.CANCELLED;
                        page.status = "cancelled";
                        continue;
                    }

                    page.status = "running";
                    try (ResourceResolver resourceResolver = callResourceResolver.clone(null)) {
                        resourceResolver.revert();
                        resourceResolver.refresh();
                        Resource resource = resourceResolver.getResource(page.resourcePath);
                        try {
                            if (resource != null) {
                                AutoPageTranslateService.Stats stats = pageTranslateService.translateLiveCopy(resource, translationParameters);
                                page.stats = stats;
                                page.status = stats.hasChanges() ? "done" : "unchanged";
                            }
                        } catch (GPTException.GPTUserNotificationException e) {
                            throw e;
                        } catch (Exception e) {
                            resourceResolver.revert();
                            resourceResolver.refresh();
                            // mark translation as failed.
                            resource.adaptTo(ModifiableValueMap.class).put(AI_TRANSLATION_ERRORMARKER, Calendar.getInstance());
                            resourceResolver.commit();
                            throw e;
                        }
                    } catch (GPTException.GPTUserNotificationException e) {
                        page.status = "cancelled - user notification";
                        this.messages.append(e.getMessage()).append("\n\n");
                        LOG.info("User notification during translation of " + page.pagePath + ": " + e.getMessage());
                        hasErrors = true; // not quite true but we don't want an 'OK, translation done'
                    } catch (Exception e) {
                        page.status = "error";
                        this.messages.append("Error translating ").append(page.pagePath).append(": ").append(e).append("\n");
                        LOG.error("Error translating " + page.pagePath, e);
                        hasErrors = true;
                    }
                }
                status = hasErrors ? TranslationStatus.DONE_WITH_ERRORS : interrupted ? TranslationStatus.CANCELLED : TranslationStatus.FINISHED;
            } catch (InterruptedException e) {
                LOG.error("Interruption during " + this, e);
                Thread.currentThread().interrupt();
                status = TranslationStatus.INTERRUPTED;
            } catch (Exception e) {
                LOG.error("Error during " + this, e);
                status = TranslationStatus.ERROR;
                messages.append("Error: ").append(e).append("\n");
            } finally {
                stopTime = new Date().toString();
                if (status == null) {
                    status = TranslationStatus.FINISHED;
                }
                future = null;
                callResourceResolver.close();
            }
        }
    }

    public static class TranslationPageImpl extends TranslationPage {
        String resourcePath;

        public TranslationPageImpl(Resource resource) {
            this.resourcePath = resource.getPath();
            pagePath = ResourceUtil.getParent(resourcePath); // remove jcr:content
            this.status = "queued";
            Resource translateCopyResource = resource.getParent().getParent()
                    .getChild(resource.getParent().getName() + AutoTranslateListModel.SUFFIX_TRANSLATECOPY);
            translateCopyPagePath = translateCopyResource != null ? translateCopyResource.getPath() : null;
        }

    }

}

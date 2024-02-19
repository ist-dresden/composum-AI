package com.composum.ai.aem.core.impl.autotranslate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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

/**
 * A service that provides automatic translation of AEM pages.
 * <p>
 * This is a proof-of-concept implementation, only available if explicitly enabled in the OSGi configuration.
 */
@Designate(ocd = AutoTranslateServiceImpl.Config.class)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
// REQUIRE since this is currently only a POC
public class AutoTranslateServiceImpl implements AutoTranslateService {

    @Reference
    private ThreadPoolManager threadPoolManager;

    @Reference
    private AutoTranslateStateService stateService;

    private ThreadPool threadPool;

    private boolean disabled;

    @Override
    public List<TranslationRun> getTranslationRuns() {
        return Collections.unmodifiableList(stateService.getTranslationRuns());
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
        try {
            stateService.getTranslationRuns().forEach(TranslationRunImpl::cancel);
        } finally {
            if (threadPool != null) {
                threadPoolManager.release(threadPool);
            }
        }
    }

    protected ThreadPool getThreadPool() {
        if (threadPool == null) {
            threadPool = threadPoolManager.get(getClass().getName());
        }
        return threadPool;
    }

    @Override
    public TranslationRun startTranslation(ResourceResolver resourceResolver, String path, boolean recursive) throws LoginException {
        if (disabled) {
            throw new IllegalStateException("AutoTranslateService is disabled");
        }
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path parameter is required");
        }
        if (!path.startsWith("/content/")) {
            throw new IllegalArgumentException("Only pages below /content/ are supported: " + path);
        }

        TranslationRunImpl run = stateService.getTranslationRuns().stream()
                .filter(r -> r.rootPath.equals(path)).findAny().orElse(null);
        if (run != null) {
            throw new IllegalArgumentException("Translation run for " + path + " is already running");
        }

        ResourceResolver processResolver = resourceResolver.clone(null);
        Resource root = processResolver.getResource(path);
        if (root == null) {
            throw new IllegalArgumentException("No such resource: " + path);
        }
        List<Resource> resources;
        if (recursive) {
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

        run = new TranslationRunImpl();
        run.id = "" + Math.abs(System.nanoTime());
        run.rootPath = path;
        run.translatedPages = resources.stream().map(TranslationPageImpl::new).collect(Collectors.toList());
        stateService.getTranslationRuns().add(run);
        run.future = getThreadPool().submit(run::execute);
        run.status = "scheduled";
        return run;
    }

    protected List<Resource> collectPages(Resource root) {
        if (root.getPath().contains("/jcr:content")) {
            return Collections.singletonList(root);
        }
        // find all jcr:content nodes below root and return a list of these
        List<Resource> pages = new ArrayList<>();
        if (root.isResourceType("cq:PageContent")) {
            pages.add(root);
        } else {
            root.getChildren().forEach(child -> pages.addAll(collectPages(child)));
        }
        return pages;
    }


    public class TranslationRunImpl extends TranslationRun {
        public Future<?> future;
        List<TranslationPage> translatedPages;

        @Override
        public List<TranslationPage> getTranslatedPages() {
            return translatedPages;
        }

        public void cancel() {
            if (future != null) {
                future.cancel(true);
                future = null;
            }
        }

        public void execute() {
            status = "running";
            startTime = new Date().toString();
            for (TranslationPage page : translatedPages) {
                page.status = "running";
                // do the translation
                page.status = "done";
            }
            status = "done";
            stopTime = new Date().toString();
        }
    }

    public static class TranslationPageImpl extends TranslationPage {
        Resource page;

        public TranslationPageImpl(Resource page) {
            this.page = page;
            pagePath = page.getParent().getPath(); // remove jcr:content
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

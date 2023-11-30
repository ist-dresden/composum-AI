package com.composum.ai.backend.slingbase.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.slingbase.ApproximateMarkdownService;
import com.composum.ai.backend.slingbase.ApproximateMarkdownServicePlugin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * A plugin for the {@link com.composum.ai.backend.slingbase.ApproximateMarkdownService} that transforms the rendered
 * HTML to markdown.
 * That doesn't work for all components, but might more easily capture the text content of certain components than
 * trying to guess it from the JCR representation, as is the default.
 */
@Designate(ocd = HtmlToApproximateMarkdownServicePlugin.Config.class)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = Constants.SERVICE_RANKING + ":Integer=10000"
)
public class HtmlToApproximateMarkdownServicePlugin implements ApproximateMarkdownServicePlugin {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlToApproximateMarkdownServicePlugin.class);

    @Reference
    private AdapterManager adapterManager;

    protected Pattern allowedResourceTypePattern;
    protected Pattern deniedResourceTypePattern;

    /**
     * ResourceTypes we ignore since their rendering uses unsupported methods.
     * Blacklisting for only 1h since there might be a deployment in the meantime.
     */
    protected Cache<String, Boolean> blacklistedResourceType =
            CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();

    @NotNull
    @Override
    public PluginResult maybeHandle(
            @NotNull Resource resource, @NotNull PrintWriter out,
            @NotNull ApproximateMarkdownService service,
            @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        if (isIgnoredNode(resource)) {
            return PluginResult.NOT_HANDLED;
        }
        String resourceType = resource.getResourceType();
        if (Boolean.TRUE.equals(blacklistedResourceType.getIfPresent(resourceType) != null)) {
            return PluginResult.NOT_HANDLED;
        }

        if (allowedResourceTypePattern != null && allowedResourceTypePattern.matcher(resourceType).matches()) {
            if (deniedResourceTypePattern != null && deniedResourceTypePattern.matcher(resourceType).matches()) {
                LOG.debug("Resourcetype {} denied", resourceType);
                return PluginResult.NOT_HANDLED;
            }
            LOG.debug("Resourcetype {} allowed", resourceType);

            try {
                String html = renderedAsHTML(resource, request, response);
                String markdown = service.getMarkdown(html);
                if (StringUtils.isBlank(markdown)) {
                    LOG.debug("No markdown generated for {} with resource type {}", resource.getPath(), resource.getResourceType());
                } else {
                    LOG.debug("Markdown generated for {} with resource type {}:\n{}", resource.getPath(), resource.getResourceType(), markdown);
                    out.println(markdown);
                    out.println();
                }
                return PluginResult.HANDLED_ALL;
            } catch (ServletException | IOException | RuntimeException e) {
                if (isBecauseOfUnsupportedOperation(e)) {
                    LOG.warn("Blacklisting because of using unsupported operations: resource type {} (at {})", resource.getResourceType(), resource.getPath());
                    blacklistedResourceType.put(resourceType, true);
                    return PluginResult.NOT_HANDLED;
                }
                LOG.error("Error rendering resource {} with resource type {}", resource.getPath(), resource.getResourceType(), e);
                return PluginResult.NOT_HANDLED;
            }
        }
        return PluginResult.NOT_HANDLED;
    }

    protected boolean isBecauseOfUnsupportedOperation(Throwable e) {
        if (e instanceof UnsupportedOperationCalled) {
            return true;
        }
        if (e.getCause() != null && e.getCause() != e && isBecauseOfUnsupportedOperation(e.getCause())) {
            return true;
        }
        for (Throwable throwable : e.getSuppressed()) {
            if (throwable != e && isBecauseOfUnsupportedOperation(throwable)) {
                return true;
            }
        }
        return false;
    }

    /**
     * We start with depth 3 since the higher nodes often contain headers, navigation and such that don't help for ChatGPT.
     */
    protected boolean isIgnoredNode(@Nonnull Resource resource) {
        if (ResourceUtil.getParent(resource.getPath(), 2) == null) {
            return true;
        }
        if (resource.getName().equals("jcr:content") || resource.getParent().getName().equals("jcr:content")) {
            return true;
        }
        return false;
    }


    /**
     * We render the resource into a mock response and capture and return the generated HTML.
     * The response is wrapped so that the real response cannot be modified.
     * We don't do that for the request, because that would be more complicated and probably not needed.
     */
    protected String renderedAsHTML(Resource resource, SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        StringBuilderWriter writer = new StringBuilderWriter();
        try (PrintWriter printWriter = new PrintWriter(writer)) {
            SlingHttpServletResponse wrappedResponse = new CapturingResponse(response, printWriter, resource.getPath());
            NonModifyingRequestWrapper wrappedRequest = new NonModifyingRequestWrapper(request, resource.getPath());
            Object oldWcmAttribute = request.getAttribute("com.day.cq.wcm.api.WCMMode");
            try { // for AEM we have to avoid that edit mode introduces artifacts.
                request.removeAttribute("com.day.cq.wcm.api.WCMMode");
                request.getRequestDispatcher(resource.getPath() + ".html").include(wrappedRequest, wrappedResponse);
            } finally {
                if (oldWcmAttribute != null) {
                    request.setAttribute("com.day.cq.wcm.api.WCMMode", oldWcmAttribute);
                }
            }
            if (wrappedRequest.hadInvalidOperation) { // if that exception has been swallowed
                throw new UnsupportedOperationCalled();
            }
        }
        return writer.toString();
    }

    @Activate
    @Modified
    protected void activate(Config config) {
        this.allowedResourceTypePattern = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(config.allowedResourceTypes());
        this.deniedResourceTypePattern = AllowDenyMatcherUtil.joinPatternsIntoAnyMatcher(config.deniedResourceTypes());
        LOG.info("Allowed HTML to Markdown resource types: {}", this.allowedResourceTypePattern);
        LOG.info("Denied HTML to Markdown resource types: {}", this.deniedResourceTypePattern);
    }

    @Deactivate
    protected void deactivate() {
        this.allowedResourceTypePattern = null;
        this.deniedResourceTypePattern = null;
    }

    @ObjectClassDefinition(name = "Composum AI Html To Approximate Markdown Service Plugin", description = "A plugin for the ApproximateMarkdownService that transforms the rendered HTML of components to markdown, which can work better than trying to guess the text content from the JCR representation (as is the default) but probably doesn't work for all components. So it can be enabled for some sling resource types by regex. We will not use this for the first two levels below the page, as that could include unwanted stuff like headers and footers.")
    protected @interface Config {

        @AttributeDefinition(name = "Allowed resource types", description = "Regular expressions for allowed resource types. If not present, no resource types are allowed.") String[] allowedResourceTypes() default {".*"};

        @AttributeDefinition(name = "Denied resource types", description = "Regular expressions for denied resource types. Takes precedence over allowed resource types.") String[] deniedResourceTypes() default {};

    }

    /**
     * We wrap a response to capture the content, forwarding all but modifying methods to the original response.
     */
    protected static class CapturingResponse extends SlingHttpServletResponseWrapper {
        private final PrintWriter writer;
        private final String debuginfo;

        public CapturingResponse(SlingHttpServletResponse response, PrintWriter printWriter, String debuginfo) {
            super(response);
            this.writer = printWriter;
            this.debuginfo = debuginfo;
        }

        @Override
        public PrintWriter getWriter() {
            return writer;
        }

        protected UnsupportedOperationException logAndThrow(String error) {
            LOG.warn("Unsupported method called for {} : {}", debuginfo, error);
            throw new UnsupportedOperationException(error);
        }

        // The following methods are likely not needed; we mostly throw an exception to find whether this assumption is right.

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            throw logAndThrow("Not implemented: CapturingResponse.getOutputStream");
        }

        @Override
        public void addCookie(Cookie cookie) {
            // that might actually get a problem later, so we at least log it. Not to be expected, though.
            LOG.warn("Not implemented: CapturingResponse.addCookie {}", cookie.getName());
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            throw logAndThrow("Not implemented: CapturingResponse.sendError");
        }

        @Override
        public void sendError(int sc) throws IOException {
            throw logAndThrow("Not implemented: CapturingResponse.sendError");
        }

        @Override
        public void sendRedirect(String location) throws IOException {
            throw logAndThrow("Not implemented: CapturingResponse.sendRedirect");
        }

        @Override
        public void setDateHeader(String name, long date) {
            // ignore
        }

        @Override
        public void setHeader(String name, String value) {
            // ignore
        }

        @Override
        public void setIntHeader(String name, int value) {
            // ignore
        }

        @Override
        public void setStatus(int sc) {
            throw logAndThrow("Not implemented: CapturingResponse.setStatus");
        }

        @Override
        public void setStatus(int sc, String sm) {
            throw logAndThrow("Not implemented: CapturingResponse.setStatus");
        }

        @Override
        public int getStatus() {
            return 200;
        }

        @Override
        public void setCharacterEncoding(String charset) {
            // ignore
        }

        @Override
        public void setContentLength(int len) {
            // ignore
        }

        @Override
        public void setContentLengthLong(long len) {
            // ignore
        }

        @Override
        public void setContentType(String type) {
            // ignore
        }

        @Override
        public void setBufferSize(int size) {
            // ignore
        }

        @Override
        public void flushBuffer() throws IOException {
            // ignore
        }

        @Override
        public void reset() {
            throw logAndThrow("Not implemented: CapturingResponse.reset");
        }

        @Override
        public void resetBuffer() {
            throw logAndThrow("Not implemented: CapturingResponse.resetBuffer");
        }

        @Override
        public void setLocale(Locale loc) {
            // ignore
        }
    }

    /**
     * Wraps the request to make sure nothing is modified.
     */
    protected class NonModifyingRequestWrapper extends SlingHttpServletRequestWrapper {

        private final String debuginfo;

        protected boolean inAdaptTo;
        protected boolean hadInvalidOperation;

        /**
         * Either Object[0] for a removed attribute or new Object{attributevalue} for changed object.
         */
        private Map<String, Object[]> changedAttributes = new HashMap<>();

        public NonModifyingRequestWrapper(SlingHttpServletRequest wrappedRequest, String debuginfo) {
            super(wrappedRequest);
            this.debuginfo = debuginfo;
        }

        protected UnsupportedOperationCalled logAndThrow(String error) {
            LOG.warn("Unsupported method called for {} : {}", debuginfo, error);
            hadInvalidOperation = true;
            throw new UnsupportedOperationCalled();
        }

        // Methods we think are too dangerous to use since they might modify the request, so we mostly throw an exception.
        // Possibly we'll have to rethink this.
        @Nullable
        @Override
        public RequestDispatcher getRequestDispatcher(@NotNull String path, RequestDispatcherOptions options) {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.getRequestDispatcher");
        }

        @Nullable
        @Override
        public RequestDispatcher getRequestDispatcher(@NotNull Resource resource, RequestDispatcherOptions options) {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.getRequestDispatcher");
        }

        @Nullable
        @Override
        public RequestDispatcher getRequestDispatcher(@NotNull Resource resource) {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.getRequestDispatcher");
        }

        @Override
        public HttpSession getSession(boolean create) {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.getSession");
        }

        @Override
        public HttpSession getSession() {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.getSession");
        }

        @Override
        public String changeSessionId() {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.changeSessionId");
        }

        @Override
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.authenticate");
        }

        @Override
        public void login(String username, String password) throws ServletException {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.login");
        }

        @Override
        public void logout() throws ServletException {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.logout");
        }

        @Override
        public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.upgrade");
        }

        @Override
        public void setCharacterEncoding(String env) {
            LOG.debug("ignoring NonModifyingRequestWrapper.setCharacterEncoding {}", env);
            // ignore, though somewhat doubtfully
        }

        @Override
        public void setAttribute(String name, Object o) {
            LOG.trace("emulating NonModifyingRequestWrapper.setAttribute {} for {}", name, debuginfo);
            changedAttributes.put(name, new Object[]{o});
        }

        @Override
        public void removeAttribute(String name) {
            LOG.trace("emulating NonModifyingRequestWrapper.removeAttribute {} for {}", name, debuginfo);
            changedAttributes.put(name, new Object[0]);
        }

        @Override
        public Object getAttribute(String name) {
            Object[] change = changedAttributes.get(name);
            if (change != null) {
                if (change.length == 0) {
                    return null;
                } else {
                    return change[0];
                }
            }
            return super.getAttribute(name);
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.startAsync");
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.startAsync");
        }

        @Override
        public AsyncContext getAsyncContext() {
            throw logAndThrow("Not implemented: NonModifyingRequestWrapper.getAsyncContext");
        }

        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (inAdaptTo) {
                throw logAndThrow("Loop in NonModifyingRequestWrapper.adaptTo " + type);
            }
            try {
                inAdaptTo = true; // make sure the adaptermanager doesn't just call adaptTo again - we'll have to give up then.
                return adapterManager.getAdapter(this, type);
            } finally {
                inAdaptTo = false;
            }
        }

    }

    /**
     * Thrown when unsupported operation was called that requires blacklisting.
     */
    protected static class UnsupportedOperationCalled extends RuntimeException {
        // empty
    }
}

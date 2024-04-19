package com.composum.ai.composum.bundle;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.XSS;

/**
 * Servlet that serves the dialogs.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Dialog Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=" + AIDialogServlet.SERVLET_PATH,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class AIDialogServlet extends AbstractServiceServlet {

    public static final String SERVLET_PATH = "/bin/cpm/ai/dialog";

    private static final Logger LOG = LoggerFactory.getLogger(AIDialogServlet.class);

    public enum Extension {json, html}

    public enum Operation {translationDialog, categorizeDialog, creationDialog, sidebarDialog}

    protected final ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        // e.g. http://localhost:9090/bin/cpm/ai/dialog.translationDialog.html/content/ist/composum/home/platform/_jcr_content/_jcr_description
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html, Operation.translationDialog, new ShowDialogOperation(Operation.translationDialog, "composum/pages/options/ai/dialogs/translate"));
        // e.g. http://localhost:9090/bin/cpm/ai/dialog.categorizeDialog.html/content/ist/composum/home/platform/_jcr_content/category
        // or http://localhost:9090/bin/cpm/ai/dialog.categorizeDialog.suggestions.html/content/ist/composum/home/platform/_jcr_content/category
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html, Operation.categorizeDialog, new ShowDialogOperation(Operation.categorizeDialog, "composum/pages/options/ai/dialogs/categorize"));
        // e.g. http://localhost:9090/bin/cpm/ai/dialog.creationDialog.html/content/ist/composum/home/platform/_jcr_content/_jcr_description
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html, Operation.creationDialog, new ShowDialogOperation(Operation.creationDialog, "composum/pages/options/ai/dialogs/create"));

        // primarily for help http://localhost:9090/bin/cpm/ai/dialog.sidebarDialog.help.html
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html, Operation.sidebarDialog, new ShowDialogOperation(Operation.sidebarDialog, "composum/pages/options/ai/tools/sidebar"));
    }

    /**
     * Shows the corresponding dialog.
     */
    protected class ShowDialogOperation implements ServletOperation {

        private final Operation operation;
        private final String dialogResourceType;

        public ShowDialogOperation(Operation operation, String dialogResourceType) {
            this.operation = operation;
            this.dialogResourceType = dialogResourceType;
        }

        @Override
        public void doIt(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, @Nullable ResourceHandle resource) throws RepositoryException, IOException, ServletException {
            final String path = XSS.filter(request.getRequestPathInfo().getSuffix());
            if (StringUtils.isNotBlank(path)) {
                LOG.info("Showing dialog for path '{}'", path);
                final RequestDispatcherOptions options = new RequestDispatcherOptions();
                options.setForceResourceType(dialogResourceType);

                final String selectors = Arrays.stream(request.getRequestPathInfo().getSelectors())
                        .filter(s -> !operation.name().equals(s))
                        .collect(Collectors.joining("."));
                options.setReplaceSelectors(StringUtils.isNotBlank(selectors) ? selectors : null);

                final RequestDispatcher dispatcher = request.getRequestDispatcher(resource, options);
                if (dispatcher != null) {
                    dispatcher.forward(request, response);
                    return;
                }
            }
            LOG.info("No dialog for path '{}'", path);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

}

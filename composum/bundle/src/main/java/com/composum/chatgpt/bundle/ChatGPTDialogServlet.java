package com.composum.chatgpt.bundle;

import java.io.IOException;

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

import com.composum.pages.commons.util.RequestUtil;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.util.XSS;

/**
 * Servlet that serves the dialogs and processes the reactions, if they aren't handled within Javascript itself.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum ChatGPT Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/platform/chatgpt/dialog",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class ChatGPTDialogServlet extends AbstractServiceServlet {


    private static final Logger LOG = LoggerFactory.getLogger(ChatGPTDialogServlet.class);

    public enum Extension {json, html}

    public enum Operation {translationDialog}

    protected final ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        // e.g. http://localhost:9090/bin/cpm/platform/chatgpt/dialog.translationDialog.html/content/ist/composum/home/platform/_jcr_content/_jcr_description
        operations.setOperation(ServletOperationSet.Method.GET, Extension.html, Operation.translationDialog, new ShowDialogOperation("composum/chatgpt/pagesintegration/dialogs/translate"));
    }

    /**
     * Shows the corresponding dialog.
     */
    protected class ShowDialogOperation implements ServletOperation {

        private final String dialogResourceType;

        public ShowDialogOperation(String dialogResourceType) {
            this.dialogResourceType = dialogResourceType;
        }

        @Override
        public void doIt(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response, @Nullable ResourceHandle resource) throws RepositoryException, IOException, ServletException {
            final String path = XSS.filter(request.getRequestPathInfo().getSuffix());
            if (StringUtils.isNotBlank(path)) {
                LOG.info("Showing dialog for path '{}'", path);
                final RequestDispatcherOptions options = new RequestDispatcherOptions();
                options.setForceResourceType(dialogResourceType);

                final String selectors = RequestUtil.getSelectorString(request, null, 1);
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

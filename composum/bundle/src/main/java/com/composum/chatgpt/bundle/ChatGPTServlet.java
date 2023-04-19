package com.composum.chatgpt.bundle;

import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTChatRequest;
import com.composum.chatgpt.base.service.chat.GPTContentCreationService;
import com.composum.chatgpt.base.service.chat.GPTMessageRole;
import com.composum.chatgpt.base.service.chat.GPTTranslationService;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;

/**
 * Servlet providing the various services from the backend as servlet.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum ChatGPT Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/platform/chatgpt/servlet",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class ChatGPTServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ChatGPTServlet.class);

    @Reference
    protected GPTChatCompletionService chatService;

    @Reference
    protected GPTTranslationService translationService;

    @Reference
    protected GPTContentCreationService contentCreationService;

    public enum Extension {json}

    public enum Operation {hello}

    protected final ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        operations.setOperation(ServletOperationSet.Method.GET, Extension.json, Operation.hello,
                new HelloOperation());
    }

    /**
     * A quick hello world, http://localhost:9090/bin/cpm/platform/chatgpt/servlet.hello.json
     */
    public class HelloOperation implements ServletOperation {

        @Override
        public void doIt(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response, @Nonnull ResourceHandle resource) throws IOException, ServletException {
            response.setContentType("text/plain");
            try (PrintWriter writer = response.getWriter()) {
                writer.write("Hello World!");
                writer.write("Trying to access ChatGPT...");
                writer.flush();
                GPTChatRequest chatrequest = new GPTChatRequest();
                chatrequest.addMessage(GPTMessageRole.USER, "Hi!");
                String chatresponse = chatService.getSingleChatCompletion(chatrequest);
                writer.write("ChatGPT says: " + chatresponse);
            }
        }
    }


}
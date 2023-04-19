package com.composum.chatgpt.bundle;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.GPTException;
import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTContentCreationService;
import com.composum.chatgpt.base.service.chat.GPTTranslationService;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.servlet.Status;

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

    public enum Operation {translate, keywords, description, prompt}

    protected final ServletOperationSet<Extension, Operation> operations = new ServletOperationSet<>(Extension.json);

    @Override
    protected ServletOperationSet<Extension, Operation> getOperations() {
        return operations;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        // FIXME(hps,19.04.23) only use POST later, but for now, GET is easier to test
        for (ServletOperationSet.Method method : List.of(ServletOperationSet.Method.GET, ServletOperationSet.Method.POST)) {
            operations.setOperation(method, Extension.json, Operation.translate,
                    new TranslateOperation());
            operations.setOperation(method, Extension.json, Operation.keywords,
                    new KeywordsOperation());
            operations.setOperation(method, Extension.json, Operation.description,
                    new DescriptionOperation());
            operations.setOperation(method, Extension.json, Operation.prompt,
                    new PromptOperation());
        }
    }

    protected abstract class AbstractGPTServletOperation implements ServletOperation {

        /**
         * Frame implementation with common code
         */
        @Override
        public final void doIt(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response, @Nullable ResourceHandle resource) throws RepositoryException, IOException, ServletException {
            Status status = new Status(request, response, LOG);

            try {
                performOperation(status, request, response);
            } catch (GPTException e) {
                status.error("Error accessing ChatGPT", e);
                status.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (RuntimeException e) {
                status.error("Internal error", e);
                status.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            status.sendJson();
        }

        protected abstract void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response);

        protected Integer getOptionalInt(Status status, SlingHttpServletRequest request, String parameterName) {
            String parameter = request.getParameter(parameterName);
            if (parameter != null) {
                try {
                    return Integer.parseInt(parameter);
                } catch (NumberFormatException e) {
                    status.error("Invalid nonnumeric value for parameter " + parameterName + ": " + parameter);
                }
            }
            return null;
        }
    }

    /**
     * Servlet representation of {@link GPTTranslationService}, specifically {@link GPTTranslationService#singleTranslation(String, String, String)}
     * with arguments text, sourceLanguage, targetLanguage .
     * Input are the parameters text, sourceLanguage, targetLanguage, output is in data.result.translation
     * a list containing the translation as (currently) a single string.
     * We use a list since it might be sensible to create multiple translation variants in the future, if requested.
     */
    // http://localhost:9090/bin/cpm/platform/chatgpt/servlet.translate.json?sourceLanguage=en&targetLanguage=de&text=Hello%20World
    public class TranslateOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String text = status.getRequiredParameter("text", null, "No text to translate");
            String sourceLanguage = status.getRequiredParameter("sourceLanguage", null, "No source language");
            String targetLanguage = status.getRequiredParameter("targetLanguage", null, "No target language");
            if (status.isValid()) {
                String translation = translationService.singleTranslation(text, sourceLanguage, targetLanguage);
                status.data("result").put("translation", List.of(translation));
            }
        }

    }

    /**
     * Servlet representation of {@link GPTContentCreationService#generateKeywords(String)} with argument text.
     * Input parameters is text, output is in data.result.keywords a list of keywords.
     */
    public class KeywordsOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String text = status.getRequiredParameter("text", null, "No text given");
            if (status.isValid()) {
                List<String> result = contentCreationService.generateKeywords(text);
                status.data("result").put("keywords", result);
            }
        }

    }

    /**
     * Servlet representation of {@link GPTContentCreationService#generateDescription(String, int)} with arguments text and maxwords.
     * Input parameters is text and the optional numeric parameter maxwords,
     * output is in data.result.description a string containing the description.
     */
    public class DescriptionOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String text = status.getRequiredParameter("text", null, "No text given");
            Integer maxwords = getOptionalInt(status, request, "maxwords");
            if (status.isValid()) {
                String result = contentCreationService.generateDescription(text, maxwords != null ? maxwords : -1);
                status.data("result").put("description", result);
            }
        }

    }

    /**
     * Servlet representation of {@link com.composum.chatgpt.base.service.chat.GPTContentCreationService#executePrompt(java.lang.String, int)}
     * with arguments prompt and maxwords.
     * Input parameters is text and the optional numeric parameter maxwords,
     * output is in data.result.text the generated text.
     */
    public class PromptOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String prompt = status.getRequiredParameter("prompt", null, "No prompt given");
            Integer maxwords = getOptionalInt(status, request, "maxwords");
            if (status.isValid()) {
                String result = contentCreationService.executePrompt(prompt, maxwords != null ? maxwords : -1);
                status.data("result").put("text", result);
            }
        }

    }

    /**
     * Servlet representation of {@link com.composum.chatgpt.base.service.chat.GPTContentCreationService#executePromptOnText(java.lang.String, java.lang.String, int)}
     * with arguments prompt, text and maxwords.
     * Input parameters is a text, the prompt to execute on it and the optional numeric parameter maxwords,
     * output is in data.result.text the generated text.
     */
    public class PromptOnTextOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String prompt = status.getRequiredParameter("prompt", null, "No prompt given");
            String text = status.getRequiredParameter("text", null, "No text given");
            Integer maxwords = getOptionalInt(status, request, "maxwords");
            if (status.isValid()) {
                String result = contentCreationService.executePromptOnText(prompt, text, maxwords != null ? maxwords : -1);
                status.data("result").put("text", result);
            }
        }

    }

}

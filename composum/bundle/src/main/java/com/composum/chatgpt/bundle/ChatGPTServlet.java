package com.composum.chatgpt.bundle;

import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.chatgpt.base.service.GPTException;
import com.composum.chatgpt.base.service.chat.GPTChatCompletionService;
import com.composum.chatgpt.base.service.chat.GPTContentCreationService;
import com.composum.chatgpt.base.service.chat.GPTTranslationService;
import com.composum.chatgpt.bundle.model.ChatGPTTranslationDialogModel;
import com.composum.sling.core.BeanContext;
import com.composum.sling.core.ResourceHandle;
import com.composum.sling.core.servlet.AbstractServiceServlet;
import com.composum.sling.core.servlet.ServletOperation;
import com.composum.sling.core.servlet.ServletOperationSet;
import com.composum.sling.core.servlet.Status;
import com.composum.sling.core.util.XSS;

/**
 * Servlet providing the various services from the backend as servlet, which are useable for the authors.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum ChatGPT Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/platform/chatgpt/authoring",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class ChatGPTServlet extends AbstractServiceServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ChatGPTServlet.class);

    /**
     * Parameter to transmit a text on which ChatGPT is to operate - not as instructions but as data.
     */
    public static final String PARAMETER_TEXT = "text";

    /**
     * Parameter to transmit a prompt on which ChatGPT is to operate - that is, the instructions.
     */
    public static final String PARAMETER_PROMPT = "prompt";

    /**
     * Optional numerical parameter limiting the number of words to be generated. That might lead to cutoff or actual wordcount, depending on the operation, and is usually only quite approximate.
     */
    public static final String PARAMETER_MAXWORDS = "maxwords";

    /**
     * The path to a resource, given as parameter.
     */
    public static final String PARAMETER_PATH = "path";

    /**
     * Property name, given as parameter.
     */
    public static final String PARAMETER_PROPERTY = "property";

    /**
     * Key for {@link Status#data(String)} - toplevel key in the servlet result.
     */
    public static final String RESULTKEY = "result";

    /**
     * Key in the result that transmits the generated description.
     */
    public static final String RESULTKEY_DESCRIPTIION = "description";

    /**
     * Key in the result that transmits the generated list of keywords.
     */
    public static final String RESULTKEY_KEYWORDS = "keywords";

    /**
     * Key in the result that transmits the generated text.
     */
    public static final String RESULTKEY_TEXT = "text";

    /**
     * Key in the result that transmits a list of translations (currently only one, but might be extended later.)
     */
    public static final String RESULTKEY_TRANSLATION = "translation";


    @Reference
    protected GPTChatCompletionService chatService;

    @Reference
    protected GPTTranslationService translationService;

    @Reference
    protected GPTContentCreationService contentCreationService;

    @Reference
    protected ApproximateMarkdownService markdownService;

    protected BundleContext bundleContext;

    public enum Extension {json}

    public enum Operation {translate, keywords, description, prompt, create}

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
            operations.setOperation(method, Extension.json, Operation.create,
                    new CreateOperation());
        }
    }

    @Activate
    public void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
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
    // http://localhost:9090/bin/cpm/platform/chatgpt/authoring.translate.json?sourceLanguage=en&targetLanguage=de&text=Hello%20World
    public class TranslateOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String text = XSS.filter(request.getParameter(PARAMETER_TEXT));
            String path = request.getParameter(PARAMETER_PATH);
            if (isBlank(path)) {
                path = request.getRequestPathInfo().getSuffix();
            }
            String property = request.getParameter(PARAMETER_PROPERTY);
            if (isBlank(text) && isAnyBlank(path, property)) {
                status.error("No text or path *and* property given");
            }
            String sourceLanguage = status.getRequiredParameter("sourceLanguage", null, "No sourceLanguage given");
            String targetLanguage = request.getParameter("targetLanguage");
            if (isNoneBlank(path, property)) {
                Resource propertyResource = request.getResourceResolver().getResource(path + '/' + property);
                if (propertyResource == null) {
                    status.error("No resource found at " + path + '/' + property);
                } else {
                    BeanContext context = new BeanContext.Servlet(getServletContext(), bundleContext, request, response);
                    ChatGPTTranslationDialogModel model = context.withResource(propertyResource).adaptTo(ChatGPTTranslationDialogModel.class);
                    if (model == null) {
                        status.error("Could not read from " + path + '/' + property);
                    } else {
                        text = model.getValueForLanguage(sourceLanguage);
                        if (isBlank(text)) {
                            status.error("No text found for language " + sourceLanguage + " at " + path + '/' + property);
                        }
                        if (isBlank(targetLanguage)) {
                            targetLanguage = model.getLanguageKey(); // pages default
                        }
                    }
                }
            }
            if (isBlank(targetLanguage)) {
                status.error("No targetLanguage given, and it could not be determined from context.");
            }
            if (status.isValid()) {
                String translation = translationService.singleTranslation(text, sourceLanguage, targetLanguage);
                translation = XSS.filter(translation);
                status.data(RESULTKEY).put(RESULTKEY_TRANSLATION, List.of(translation));
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
            String text = status.getRequiredParameter(PARAMETER_TEXT, null, "No text given");
            if (status.isValid()) {
                List<String> result = contentCreationService.generateKeywords(text);
                result = result.stream().map(XSS::filter).collect(Collectors.toList());
                status.data(RESULTKEY).put(RESULTKEY_KEYWORDS, result);
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
            String text = status.getRequiredParameter(PARAMETER_TEXT, null, "No text given");
            Integer maxwords = getOptionalInt(status, request, PARAMETER_MAXWORDS);
            if (status.isValid()) {
                String result = contentCreationService.generateDescription(text, maxwords != null ? maxwords : -1);
                result = XSS.filter(result);
                status.data(RESULTKEY).put(RESULTKEY_DESCRIPTIION, result);
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
            String prompt = status.getRequiredParameter(PARAMETER_PROMPT, null, "No prompt given");
            Integer maxwords = getOptionalInt(status, request, PARAMETER_MAXWORDS);
            if (status.isValid()) {
                String result = contentCreationService.executePrompt(prompt, maxwords != null ? maxwords : -1);
                result = XSS.filter(result);
                status.data(RESULTKEY).put(RESULTKEY_TEXT, result);
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
            String prompt = status.getRequiredParameter(PARAMETER_PROMPT, null, "No prompt given");
            String text = status.getRequiredParameter(PARAMETER_TEXT, null, "No text given");
            Integer maxwords = getOptionalInt(status, request, PARAMETER_MAXWORDS);
            if (status.isValid()) {
                String result = contentCreationService.executePromptOnText(prompt, text, maxwords != null ? maxwords : -1);
                result = XSS.filter(result);
                status.data(RESULTKEY).put(RESULTKEY_TEXT, result);
            }
        }

    }

    /* - Parameter names for the inputs and outputs:
      - Prompt Textarea: prompt
      - Content Selector: contentSelect
      - Text Length Selector: textLength
*/

    /**
     * Implements the content creation operation. Input parameters are:
     * - prompt: the prompt to execute
     * - textLength: the maximum length of the generated text. If it starts with a number and a | then the number is
     * used as maxwords parameter, limiting the number of tokens, and the rest is transmitted in the prompt to ChatGPT.
     * - inputPath: if a path is given, the markdown generated by the path is used as input
     * - inputText: alternatively to the path, this text is used as input
     * Output is in data.result.text the generated text.
     */
    public class CreateOperation extends AbstractGPTServletOperation {

        @Override
        protected void performOperation(Status status, SlingHttpServletRequest request, SlingHttpServletResponse response) {
            String prompt = status.getRequiredParameter(PARAMETER_PROMPT,
                    Pattern.compile("(?s).*\\S.*"), "No prompt given");
            String textLength = request.getParameter("textLength");
            String inputPath = request.getParameter("inputPath");
            String inputText = request.getParameter("inputText");
            if (isNoneBlank(inputPath, inputText)) {
                status.error("Both inputPath and inputText given, only one of them is allowed");
            }
            int maxwords = 300; // some arbitrary default
            if (isNotBlank(textLength)) {
                Matcher matcher = Pattern.compile("\\s*(\\d+)\\s*\\|\\s*(.*)").matcher(textLength);
                if (matcher.matches()) {
                    maxwords = Integer.parseInt(matcher.group(1));
                    textLength = matcher.group(2) + "\n\n" + prompt;
                }
            }
            if (status.isValid()) {
                String fullPrompt = prompt;
                if (isNotBlank(textLength)) {
                    fullPrompt = textLength + "\n\n" + prompt;
                }
                if (isNotBlank(inputPath)) {
                    Resource resource = request.getResourceResolver().getResource(inputPath);
                    if (resource == null) {
                        status.error("No resource found at " + inputPath);
                    } else {
                        inputText = markdownService.approximateMarkdown(resource);
                    }
                }
                if (status.isValid()) {
                    String result;
                    if (isNotBlank(inputText)) {
                        result = contentCreationService.executePromptOnText(fullPrompt, inputText, maxwords);
                    } else {
                        result = contentCreationService.executePrompt(fullPrompt, maxwords);
                    }
                    result = XSS.filter(result);
                    status.data(RESULTKEY).put(RESULTKEY_TEXT, result);
                }
            }
        }
    }

}

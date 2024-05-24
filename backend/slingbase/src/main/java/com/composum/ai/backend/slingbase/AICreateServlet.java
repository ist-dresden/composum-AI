package com.composum.ai.backend.slingbase;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.composum.ai.backend.base.service.chat.GPTChatMessage;
import com.composum.ai.backend.base.service.chat.GPTChatRequest;
import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTContentCreationService;
import com.composum.ai.backend.base.service.chat.GPTMessageRole;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Servlet providing the various services from the backend as servlet, which are useable for the authors.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Backend Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/create",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_GET,
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class AICreateServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AICreateServlet.class);

    /**
     * Parameter to transmit a text on which ChatGPT is to operate - not as instructions but as data.
     */
    public static final String PARAMETER_SOURCE = "source";

    /**
     * Parameter with a JCR path that is used as input text on which ChatGPT is to operate - not as instructions but as data.
     */
    public static final String PARAMETER_SOURCEPATH = "sourcePath";

    /**
     * Parameter to transmit a prompt on which ChatGPT is to operate - that is, the instructions.
     */
    public static final String PARAMETER_PROMPT = "prompt";

    /**
     * Parameter to transmit additional chat after the first prompt {@link #PARAMETER_PROMPT}. Format: array of serialized
     * {@link GPTChatMessage}.
     * E.g. <code>[{"role":"assistant","content":"That's good."}, {"role":"user","content":"Why exactly?"}, ]</code>.
     */
    public static final String PARAMETER_CHAT = "chat";

    /**
     * Optional numerical parameter limiting the number of tokens (about 3/4 english word on average) to be generated.
     * That might lead to cutoff, as this is a hard limit and ChatGPT doesn't know about that during generation.
     * So it's advisable to specify the desired text length in the prompt, too. - Note there is an alternative in
     * {@link #PARAMETER_TEXTLENGTH}.
     */
    public static final String PARAMETER_MAXTOKENS = "maxtokens";

    /**
     * Optional boolean parameter that indicates the inputText and response are in HTML, not Markdown.
     */
    public static final String PARAMETER_RICHTEXT = "richText";

    /**
     * Description of intended response (generated text) length, optionally including maximum number of tokens,
     * as e.g. in "1000|Several paragraphs of text".
     */
    public static final String PARAMETER_TEXTLENGTH = "textLength";

    /**
     * Parameter to transmit a path to an image instead of a text.
     */
    public static final String PARAMETER_INPUT_IMAGE_PATH = "inputImagePath";

    /**
     * Session contains a map at this key that maps the streamids to the streaming handle.
     */
    public static final String SESSIONKEY_STREAMING = AICreateServlet.class.getName() + ".streaming";

    /**
     * The ID of the stream.
     */
    public static final String PARAMETER_STREAMID = "streamid";

    /**
     * Parameter containing the path of the page, for determining the configuration.
     */
    public static final String PARAMETER_CONFIGBASEPATH = "configBasePath";

    @Reference
    protected GPTContentCreationService contentCreationService;

    @Reference
    protected ApproximateMarkdownService markdownService;

    @Reference
    protected AIConfigurationService configurationService;

    protected BundleContext bundleContext;

    protected Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    @Activate
    public void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Saves stream for streaming responses into session, to be retrieved with {@link #retrieveStream(String, SlingHttpServletRequest)}.
     */
    @SuppressWarnings("unchecked")
    protected String saveStream(EventStream stream, SlingHttpServletRequest request) {
        String streamId = UUID.randomUUID().toString();
        Map<String, EventStream> streams = (Map<String, EventStream>) request.getSession().getAttribute(SESSIONKEY_STREAMING);
        if (streams == null) {
            streams = new LinkedHashMap<>();
            request.getSession().setAttribute(SESSIONKEY_STREAMING, streams);
        }
        if (streams.size() > 5) { // normally that should be cleaned up automatically by retrieveStream, just to be sure.
            streams.remove(streams.keySet().iterator().next());
        }
        streams.put(streamId, stream);
        stream.setId(streamId);
        return streamId;
    }

    protected EventStream retrieveStream(String streamId, SlingHttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, EventStream> streams = (Map<String, EventStream>) request.getSession().getAttribute(SESSIONKEY_STREAMING);
        if (streams == null) {
            return null;
        }
        EventStream stream = streams.get(streamId);
        streams.remove(streamId); // using it more than once would lead to conflicts.
        return stream;
    }

    /**
     * Returns an event stream that was prepared by a previous operation, as a second request after a POST request returning
     * a 202 with a 'Location' header  to this servlet, since only GET requests are supported by the EventStream class in browser.
     * The event stream is stored in the session under the key {@link #SESSIONKEY_STREAMING} and is removed after
     * the request.
     * <p>
     * In the event stream the generated response is put into 'data' . When the creation is finished, we create an event
     * event 'finished' into the stream with data JSON like this: {"success":true,"data":{"result":{"finishreason":"STOP"}}}
     * In case of errors, there will be an 'exception' event into the stream with data JSON like this: {"success":false,"title":"Internal error","messages":[{"level":"error","text":"something happened"}]}
     */
    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws IOException, ServletException {
        String streamId = request.getParameter(PARAMETER_STREAMID);
        LOG.info("Retrieving stream {}", streamId);
        EventStream stream = retrieveStream(streamId, request);
        if (stream == null) {
            response.sendError(HttpServletResponse.SC_GONE, "Stream " + streamId + " not found (anymore?)");
        } else {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/event-stream");
            response.setHeader("Cache-Control", "no-cache");
            try (PrintWriter writer = response.getWriter()) {
                stream.writeTo(writer);
                if (stream.getWholeResponse() != null) {
                    LOG.debug("Whole response for {} : {}", streamId, stream.getWholeResponse());
                }
            } catch (InterruptedException e) {
                LOG.warn("Interrupted writing to stream " + streamId, e);
                Thread.currentThread().interrupt();
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Interrupted during writing stream " + e);
            } catch (IOException e) {
                LOG.warn("Error writing to stream " + streamId, e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error writing stream " + e);
            }
        }
    }

    protected Integer getOptionalInt(SlingHttpServletRequest request, SlingHttpServletResponse response, String parameterName) throws IOException {
        String parameter = request.getParameter(parameterName);
        if (parameter != null) {
            try {
                return Integer.parseInt(parameter);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter " + parameterName + " must be a number");
                throw new IllegalArgumentException("Parameter " + parameterName + " must be a number");
            }
        }
        return null;
    }

    protected String getMandatoryParameter(SlingHttpServletRequest request, SlingHttpServletResponse response, String parameterName) throws IOException {
        String parameter = request.getParameter(parameterName);
        if (parameter == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter " + parameterName + " is mandatory");
            throw new IllegalArgumentException("Parameter " + parameterName + " is mandatory");
        }
        return parameter;
    }

    /**
     * Implements the content creation operation. Input parameters are:
     * <ul>
     *     <li>prompt: the prompt to execute</li>
     *     <li>textLength: the maximum length of the generated text. If it starts with a number and a | then the number is
     *     used as maxwords parameter, limiting the number of tokens, and the rest is transmitted in the prompt to ChatGPT.</li>
     *     <li>inputPath: if a path is given, the markdown generated by the path is used as input</li>
     *     <li>inputText: alternatively to the path, this text is used as input</li>
     *     <li>chat: additional chat history before the prompt, as serialized {@link GPTChatMessage} array</li>
     *     <li>richText: if true, the inputText and response are in HTML, not Markdown</li>
     *     <li>maxtokens: optional numerical parameter limiting the number of tokens (about 3/4 english word on average) to be generated.
     *     That might lead to cutoff, as this is a hard limit and ChatGPT doesn't know about that during generation.
     *     So it's advisable to specify the desired text length in the prompt, too.</li>
     * </ul>
     * A successful response will return an HTTP 200 with a JSON map with a {@value #PARAMETER_STREAMID} with an streamid
     * to access the response stream, which can be used with {@link #doGet(SlingHttpServletRequest, SlingHttpServletResponse)}
     * to deliver the result as event stream.
     * Otherwise, it'll normally be an HTTP 400 with an error message.
     */
    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        LOG.info("Starting content creation");
        String prompt = getMandatoryParameter(request, response, PARAMETER_PROMPT);
        String textLength = request.getParameter(PARAMETER_TEXTLENGTH);
        String sourcePath = request.getParameter(PARAMETER_SOURCEPATH);
        String sourceText = request.getParameter(PARAMETER_SOURCE);
        String configBasePath = request.getParameter(PARAMETER_CONFIGBASEPATH);
        String inputImagePath = request.getParameter(PARAMETER_INPUT_IMAGE_PATH);
        if ("undefined".equals(inputImagePath) || "null".equals(inputImagePath) || StringUtils.isBlank(inputImagePath)) {
            inputImagePath = null;
        }
        GPTConfiguration config = configurationService.getGPTConfiguration(request.getResourceResolver(), configBasePath);
        String chat = request.getParameter(PARAMETER_CHAT);
        if (Stream.of(sourcePath, sourceText, inputImagePath).filter(StringUtils::isNotBlank).count() > 1) {
            LOG.warn("More than one of sourcePath and sourceText and " + PARAMETER_INPUT_IMAGE_PATH +
                    " given, only one of them is allowed");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "More than one of sourcePath and sourceText and " + PARAMETER_INPUT_IMAGE_PATH +
                    " given, only one of them is allowed");
            return;
        }
        boolean richtext = Boolean.TRUE.toString().equalsIgnoreCase(request.getParameter(PARAMETER_RICHTEXT));
        GPTConfiguration mergedConfig = GPTConfiguration.ofRichText(richtext).merge(config);
        Integer maxTokensParam = getOptionalInt(request, response, PARAMETER_MAXTOKENS);

        int maxtokens = maxTokensParam != null ? maxTokensParam : 1000; // some arbitrary default.
        // Synchronize with cutoff in GPTChatCompletionServiceImpl as it's used in the sidebar!
        if (isNotBlank(textLength)) {
            Matcher matcher = Pattern.compile("\\s*(\\d+)\\s*\\|\\s*(.*)").matcher(textLength);
            if (matcher.matches()) { // maxtokens can be encoded into textLength, e.g. "1000|Several paragraphs of text"
                maxtokens = Integer.parseInt(matcher.group(1));
                textLength = matcher.group(2);
            }
        }
        GPTChatRequest additionalParameters = makeAdditionalParameters(maxtokens, chat, response, mergedConfig);

        String fullPrompt = prompt;
        if (isNotBlank(textLength)) {
            fullPrompt = textLength + "\n\n" + fullPrompt;
        }
        if (richtext) {
            fullPrompt = fullPrompt + "\n\n" +
                    "Important: your response must not be Markdown but be completely in HTML and begin with <p>, lists be HTML <ul> or <ol> and so forth!";
        }
        if (isNotBlank(sourcePath)) {
            Resource resource = request.getResourceResolver().getResource(sourcePath);
            if (resource == null) {
                LOG.warn("No resource found at {}", sourcePath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No resource found at " + sourcePath);
                return;
            } else {
                sourceText = markdownService.approximateMarkdown(resource, request, response);
            }
        }

        if (isNotBlank(inputImagePath)) {
            Resource resource = request.getResourceResolver().getResource(inputImagePath);
            String imageUrl = markdownService.getImageUrl(resource);
            if (imageUrl == null) {
                LOG.warn("No image found at {}", inputImagePath);
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No image found at " + inputImagePath);
                return;
            } else {
                additionalParameters.addMessages(Collections.singletonList(new GPTChatMessage(GPTMessageRole.USER, null, imageUrl)));
            }
        }

        EventStream callback = new EventStream();
        String id = saveStream(callback, request);
        LOG.info("Starting stream {}", id);
        if (isNotBlank(sourceText)) {
            contentCreationService.executePromptOnTextStreaming(fullPrompt, sourceText, additionalParameters, callback);
        } else {
            contentCreationService.executePromptStreaming(fullPrompt, additionalParameters, callback);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        // on 202 with Location header Chrome freezes in $.ajax for  AEM 6.5.7 8-{} . So we have to do it differently.
        response.setContentType("application/json");
        gson.toJson(Collections.singletonMap(PARAMETER_STREAMID, id), response.getWriter());
        LOG.info("Returning stream id {}", id);
    }

    @Nonnull
    protected GPTChatRequest makeAdditionalParameters(int maxtokens, String chat, HttpServletResponse response, GPTConfiguration config) throws IOException {
        GPTChatRequest additionalParameters = GPTChatRequest.ofMaxTokens(maxtokens).setConfiguration(config);
        if (isNotBlank(chat)) {
            additionalParameters.setConfiguration(GPTConfiguration.CHAT.merge(config));
            try {
                final Type listOfMyClassObject = new TypeToken<ArrayList<GPTChatMessage>>() {
                    // empty
                }.getType();

                List<GPTChatMessage> messages = gson.fromJson(chat, listOfMyClassObject);
                additionalParameters.addMessages(messages);
            } catch (IllegalArgumentException | JsonSyntaxException e) {
                LOG.warn("Invalid chat parameter: {}", chat, e);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid chat parameter: " + chat);
                throw e;
            }
        }
        return additionalParameters;
    }

}

package com.composum.ai.backend.slingbase;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
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

import com.composum.ai.backend.base.service.chat.GPTConfiguration;
import com.composum.ai.backend.base.service.chat.GPTDictationService;

/**
 * Servlet providing a dictation service - returns a transcribed text from an audio file.
 */
@Component(service = Servlet.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Composum AI Backend Servlet",
                ServletResolverConstants.SLING_SERVLET_PATHS + "=/bin/cpm/ai/dictate",
                ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST
        })
public class AIDictationServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AIDictationServlet.class);

    // parameters for audioStream, contentType, languageCode, prompt

    /**
     * Parameter to transmit the audio stream to be transcribed.
     */
    public static final String PARAMETER_AUDIO_STREAM = "audioStream";

    /**
     * Parameter to transmit the content type of the audio, e.g. "audio/mpeg" for mp3, "audio/wav" for wav.
     */
    public static final String PARAMETER_CONTENT_TYPE = "contentType";

    /**
     * Parameter to transmit the language code to use, e.g. "en" for English, or null for automatic detection.
     */
    public static final String PARAMETER_LANGUAGE = "language";

    /**
     * Parameter to transmit an optional prompt to give the AI some context, e.g. previous sentences.
     */
    public static final String PARAMETER_PROMPT = "prompt";

    @Reference
    protected AIConfigurationService configurationService;

    @Reference
    protected GPTDictationService dictationService;

    protected BundleContext bundleContext;

    @Activate
    public void activate(final BundleContext bundleContext) {
        this.bundleContext = bundleContext;
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
     * Returns whether dictation is enabled for the content in the suffix: status code OK means it's available, otherwise 404.
     */
    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        String suffix = request.getRequestPathInfo().getSuffix();
        GPTConfiguration config = null;
        if (suffix != null) {
            config = configurationService.getGPTConfiguration(request.getResourceResolver(), suffix);
        }
        response.setStatus(dictationService.isAvailable(config) ? HttpServletResponse.SC_OK : HttpServletResponse.SC_NOT_FOUND);
    }

    /**
     * Implements the transcription operation. Input parameters are (as a multipart form):
     * <ul>
     *     <li>file: the audio stream to transcribe</li>
     *     <li>contentType: the content type of the audio, e.g. "audio/mpeg" for mp3, "audio/wav" for wav</li>
     *     <li>language: the language code to use, e.g. "en" for English, or null / empty for automatic detection</li>
     *     <li>prompt: an optional prompt to give the AI some context, e.g. previous sentences</li>
     * </ul>
     * If there is a sling suffix in the URL, we'll use it to determine the configuration.
     * A successful response will return an HTTP 200 with the text as text/plain, UTF-8.
     * Otherwise, it'll normally be an HTTP 400 with an error message.
     */
    @Override
    protected void doPost(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            String suffix = request.getRequestPathInfo().getSuffix();
            GPTConfiguration config = null;
            if (suffix != null) {
                config = configurationService.getGPTConfiguration(request.getResourceResolver(), suffix);
            }

            if (!dictationService.isAvailable(config)) { // 404
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain");
                response.getWriter().write("Dictation service not enabled");
                return;
            }

            String contentType = getMandatoryParameter(request, response, PARAMETER_CONTENT_TYPE);
            String language = request.getParameter(PARAMETER_LANGUAGE);
            String prompt = request.getParameter(PARAMETER_PROMPT);

            InputStream inputStream = request.getRequestParameter(PARAMETER_AUDIO_STREAM).getInputStream();
            String transcription = dictationService.transcribe(inputStream, contentType, language, config, prompt);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("text/plain");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(transcription);
        } catch (Exception e) {
            LOG.error("" + e, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("text/plain");
            response.getWriter().write("Error: " + e);
        }
    }

}

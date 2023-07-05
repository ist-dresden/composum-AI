<%@page session="false" pageEncoding="UTF-8" %>
<%@ page import="static com.composum.pages.commons.PagesConstants.RA_STICKY_LOCALE" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<% request.setAttribute(RA_STICKY_LOCALE, request.getLocale()); // use editors locale %>
<cpp:element var="review" type="com.composum.ai.composum.bundle.model.SidebarDialogModel" mode="none"
             cssBase="composum-pages-options-ai-tools-sidebar" cssAdd="composum-pages-tools">
    <div class="composum-pages-tools_actions btn-toolbar text-center">
        <span class="${reviewCssBase}_dialog-title">Composum-AI</span>
        <div class="composum-pages-tools_left-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-play generate-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Triggers an AI reply - please give that a couple of seconds. You can also press enter.')}">
                </button>
                <button type="button"
                        class="fa fa-stop stop-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Aborts an AI reply')}">
                </button>
            </div>
        </div>
        <div class="composum-pages-tools_right-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-trash-o reset-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Resets this form but keeps the history.')}"></button>
                <button type="button"
                        class="fa fa-step-backward back-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Go back in the history of this dialog. You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}"></button>
                <button type="button"
                        class="fa fa-step-forward forward-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Go forward in the history of this dialog. You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}"></button>
                <button type="button"
                        class="fa fa-question help-button composum-pages-tools_button btn btn-default"
                        data-helpurl="${slingRequest.requestURI}${slingRequest.requestPathInfo.selectorString}.help.${slingRequest.requestPathInfo.extension}${slingRequest.requestPathInfo.suffix}"
                        title="${cpn:i18n(slingRequest,'Display help window')}"></button>
            </div>
        </div>
    </div>
    <div class="${reviewCssBase}_panel composum-pages-tools_panel">
        <form>
            <div class="selectors">
                <div class="form-group col-xs-6 selector"
                     title="${cpn:i18n(slingRequest,'This replaces the prompt by one of a number of predefined prompts you can use directly or use as an example for your own prompt.')}">
                    <select id="predefinedPrompts" name="predefined"
                            class="composum-pages-tools_select predefined-prompts form-control">
                        <option value=""><cpn:text i18n="true">(Predef. Prompt)</cpn:text></option>
                        <c:forEach items="${review.predefinedPrompts}" var="predefinedPrompt">
                            <option value="${predefinedPrompt.value}">${predefinedPrompt.key}</option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group col-xs-6 selector"
                     title="${cpn:i18n(slingRequest,'Select which text the AI receives in addition to your prompt, if any.')}">
                    <select id="contentSelector" name="contentSelect"
                            class="composum-pages-tools_select content-selector form-control">
                        <option value=""><cpn:text i18n="true">(Base Text)</cpn:text></option>
                        <c:forEach items="${review.contentSelectors}" var="contentSelector">
                            <option value="${contentSelector.key}">${contentSelector.value}</option>
                        </c:forEach>
                    </select>
                </div>
            </div>
            <div class="promptcontainer">
                <textarea class="form-control" name="prompt-0" rows="5"
                          placeholder="${cpn:i18n(slingRequest,'Prompt: your request to the AI. If you like some examples you can select one of the predefined prompts - that will replace the content of this field.')}"></textarea>
            </div>
            <div class="ai-response" id="response-0">
                <p>This is where the AI response will go.</p>
                <p>Another paragraph with a longer text. Another paragraph with a longer text. Another paragraph with a
                    longer text.Another paragraph with a longer text.Another paragraph with a longer text.</p>
            </div>
            <div class="promptcontainer-1">
                <textarea class="form-control" name="prompt-1" rows="3"
                          placeholder="${cpn:i18n(slingRequest,'If you like to continue this topic and have additional queries.')}"></textarea>
            </div>
        </form>
    </div>
</cpp:element>

<% request.removeAttribute(RA_STICKY_LOCALE); %>

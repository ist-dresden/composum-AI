<%@page session="false" pageEncoding="UTF-8" %>
<%@ page import="static com.composum.pages.commons.PagesConstants.RA_STICKY_LOCALE" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<% /* request.setAttribute(RA_STICKY_LOCALE, request.getLocale()); */ // use editors locale %>
<cpp:element var="review" type="com.composum.ai.composum.bundle.model.SidebarDialogModel" mode="none"
             cssBase="composum-pages-options-ai-tools-sidebar" cssAdd="composum-pages-tools">
    <%--@elvariable id="review" type="com.composum.ai.composum.bundle.model.SidebarDialogModel"--%>
    <c:if test="${review.enabled}">
        <div class="composum-pages-tools_actions btn-toolbar text-center">
            <span class="${reviewCssBase}_dialog-title">AI</span>
            <div class="composum-pages-tools_left-actions">
                <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                    <button type="button"
                            class="fa fa-play generate-button composum-pages-tools_button btn btn-default"
                            title="${cpn:i18n(slingRequest,'Submit: triggers an AI reply - please give that a couple of seconds. You can also press Control-Enter or Command-Enter in the prompt fields.')}">
                    </button>
                    <button type="button"
                            class="fa fa-stop stop-button composum-pages-tools_button btn btn-default"
                            disabled="disabled"
                            title="${cpn:i18n(slingRequest,'Stop: aborts an AI reply')}">
                    </button>
                    <button type="button"
                            class="fa fa-trash-o reset-button composum-pages-tools_button btn btn-default"
                            title="${cpn:i18n(slingRequest,'Resets this form but keeps the history.')}"></button>
                </div>
            </div>
            <div class="composum-pages-tools_right-actions">
                <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                    <button type="button"
                            class="fa fa-trash reset-history-button composum-pages-tools_button btn btn-default"
                            title="${cpn:i18n(slingRequest,'Resets this form including the whole history.')}"></button>
                    <button type="button"
                            class="fa fa-caret-left back-button composum-pages-tools_button btn btn-default"
                            title="${cpn:i18n(slingRequest,'Go back in the history of this dialog. You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}"></button>
                    <button type="button"
                            class="fa fa-caret-right forward-button composum-pages-tools_button btn btn-default"
                            title="${cpn:i18n(slingRequest,'Go forward in the history of this dialog. You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}"></button>
                    <button type="button"
                            class="fa fa-question help-button composum-pages-tools_button btn btn-default"
                            data-helpurl="${review.helpUrl}"
                            title="${cpn:i18n(slingRequest,'Display help window')}"></button>
                </div>
            </div>
        </div>
        <div class="${reviewCssBase}_panel composum-pages-tools_panel dataholder"
             data-pagepath="${review.pageContentResourcePath}" data-componentpath="${review.componentPath}">
            <div class="messages">
                <cpn:text class="alert alert-warning truncationalert" i18n="true" style="display: none;">
                    The generated text was too long and has been truncated.</cpn:text>
                <div class="alert alert-danger alert-text generalalert" style="display: none;">This is a normal alert
                </div>
            </div>

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
                        <c:forEach items="${review.contentSelectors}" var="contentSelector">
                            <option value="${contentSelector.key}">${contentSelector.value}</option>
                        </c:forEach>
                    </select>
                </div>
            </div>
            <div class="promptcontainer first">
                    <textarea class="form-control" name="prompt" rows="5"
                              placeholder="${cpn:i18n(slingRequest,'Prompt: your request to the AI. If you like some examples you can select one of the predefined prompts - that will replace the content of this field.')}"></textarea>
            </div>
            <div class="ai-response first">
                <div class="loading-indicator" style="display: none;">
                    <i class="loading-indicator-symbol fa fa-2x fa-spinner fa-pulse fa-fw"></i>
                </div>
                <c:set var="intro1"
                       value="${cpn:i18n(slingRequest,'Hi, I am the Composum Sidebar AI!')}"
                       scope="request"/>
                <c:set var="intro2"
                       value="${cpn:i18n(slingRequest,'Please give me some instructions or use one of the predefined prompts, and maybe select the text I should carry out the instructions. I can also help you through the other AI dialogs accessible through the icons in the field-labels of the component dialogs.')}"
                       scope="request"/>
                <div class="ai-response-text">${intro1}<br/>${intro2}</div>
            </div>
            <div class="promptcontainertemplate hidden">
                    <textarea class="form-control" name="prompt-1" rows="3"
                              placeholder="${cpn:i18n(slingRequest,'If you like to continue this topic and have additional queries.')}"></textarea>
            </div>
                <%-- The .promptcontainer.template and the .ai-response will be copied to add more fields during the chat, with class template removed and class chat added. --%>
        </div>
    </c:if>
    <c:if test="${!review.enabled}">
        <div class="messages">
            <cpn:text class="alert alert-warning" i18n="true">
                The Side panel AI is not enabled per configuration for this page.
            </cpn:text>
        </div>
    </c:if>
</cpp:element>

<% /* request.removeAttribute(RA_STICKY_LOCALE); */ %>

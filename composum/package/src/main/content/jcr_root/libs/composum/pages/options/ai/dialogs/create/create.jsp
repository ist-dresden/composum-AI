<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.ai.composum.bundle.model.CreateDialogModel"
             cssBase="composum-pages-options-ai-dialogs-create"
             cssAdd="dialog modal fade composum-ai-dialog extra-wide" role="dialog">
    <%--@elvariable id="model" type="com.composum.ai.composum.bundle.model.CreateDialogModel"--%>
    <div class="modal-dialog form-panel">
        <div class="modal-content">
            <form class="widget-form">
                <input name="_charset_" type="hidden" value="UTF-8"/>
                <div class="modal-header">
                    <sling:call script="buttons.jsp"/>
                    <cpn:text tagName="h4" class="modal-title dialog_title text" i18n="true">
                        Content creation assistant
                    </cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <cpn:text class="alert alert-warning truncationalert" i18n="true" style="display: none;">
                            The generated text was too long and has been truncated.</cpn:text>
                        <div class="alert alert-danger alert-text generalalert" style="display: none;"></div>
                    </div>

                    <div class="row">
                        <div class="col col-md-6">
                            <div class="panel panel-default mb-3">
                                <div class="panel-heading" title="${cpn:i18n(slingRequest,'Input Prompt')}">
                                    <cpn:text i18n="true">Input Prompt</cpn:text>
                                </div>
                                <div class="panel-body">
                                    <div class="form-group"
                                         title="${cpn:i18n(slingRequest,'This replaces the prompt (that is, the instructions to be executed by the AI) by one of a number of predefined prompts you can use directly or modify to create your own prompt.')}">
                                        <label for="predefinedPrompts">
                                            <cpn:text i18n="true">Predefined Prompts (Instructions)</cpn:text>
                                        </label>
                                        <select id="predefinedPrompts" name="predefined"
                                                class="form-control predefined-prompts">
                                            <c:forEach items="${model.predefinedPrompts}" var="predefinedPrompt">
                                                <option value="${predefinedPrompt.value}">${predefinedPrompt.key}</option>
                                            </c:forEach>
                                        </select>
                                    </div>

                                    <div class="form-group"
                                         title="${cpn:i18n(slingRequest,'Your request to the AI. If you like some examples you can select one of the predefined prompts - that will replace the content of this field.')}">
                                        <label for="promptTextarea">
                                            <cpn:text i18n="true">Input Prompt (Instructions)</cpn:text>
                                        </label>
                                        <textarea id="promptTextarea" name="prompt"
                                                  class="form-control prompt-textarea"
                                                  rows="5"></textarea>
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="col col-md-6">
                            <div class="panel panel-default mb-3">
                                <div class="panel-heading" title="${cpn:i18n(slingRequest,'Source')}">
                                    <cpn:text i18n="true">Source</cpn:text>
                                </div>
                                <div class="panel-body">
                                    <div class="form-group"
                                         title="${cpn:i18n(slingRequest,'Select which text the AI receives in addition to your prompt, if any. This can serve basically as data for your request to generate new text or modify this given text, if your prompt requires that.')}">
                                        <label for="contentSelector">
                                            <cpn:text
                                                    i18n="true">Overwrite Source Content with the text from ...</cpn:text>
                                        </label>
                                        <select id="contentSelector" name="contentSelect"
                                                class="form-control content-selector">
                                            <c:forEach items="${model.contentSelectors}" var="contentSelector">
                                                <option value="${contentSelector.key}">${contentSelector.value}</option>
                                            </c:forEach>
                                        </select>
                                    </div>

                                    <div class="form-group"
                                         title="${cpn:i18n(slingRequest,'The base text that is modified according to the prompt. Will be overwritten when the Content Selector is changed.')}">
                                        <label for="promptTextarea">
                                            <cpn:text i18n="true">Source Text</cpn:text>
                                        </label>
                                        <c:choose>
                                            <c:when test="${model.isRichText}">
                                                <cpp:widgetForm cssBase="composum-pages-options-ai-dialogs-create"
                                                                cssAdd="ai-source-field">
                                                    <cpp:widget id="sourceText" type="richtext"
                                                                hint="${cpn:i18n(slingRequest,'The base text that is modified according to the prompt. Will be overwritten when the Content Selector is changed.')}"
                                                                modelClass="com.composum.ai.composum.bundle.model.CreateDialogModel"/>
                                                </cpp:widgetForm>
                                            </c:when>
                                            <c:otherwise>
                                                <textarea id="sourcePlaintext" name="sourcePlaintext"
                                                          class="form-control source-textarea"
                                                          rows="5"></textarea>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>

                        <%-- Generate button panel --%>
                    <div class="panel panel-default mb-3">
                        <div class="panel-body align-items-center">
                            <div class="row">
                                <div class="col col-md-8 generate-container">
                                    <button type="button" class="btn btn-primary generate-button"
                                            title="${cpn:i18n(slingRequest,'Triggers the text generation - please give that a couple of seconds.')}">
                                        <cpn:text i18n="true">Generate</cpn:text>
                                    </button>
                                    <button type="button" class="btn btn-primary stop-button"
                                            disabled="disabled"
                                            title="${cpn:i18n(slingRequest,'Stops the text generation.')}">
                                        <cpn:text i18n="true">Stop</cpn:text>
                                    </button>
                                    <button type="button" class="btn btn-secondary reset-button"
                                            title="${cpn:i18n(slingRequest,'Resets this form.')}">
                                        <cpn:text i18n="true">Reset</cpn:text>
                                    </button>
                                    &nbsp;
                                    <div class="form-inline"
                                         title="${cpn:i18n(slingRequest,'The desired approximate length of the generated text.')}">
                                        <div class="form-group">
                                            <label for="textLengthSelector" class="inline-width">
                                                <cpn:text i18n="true">Text Length</cpn:text>
                                            </label>
                                            <select id="textLengthSelector" name="textLength"
                                                    class="text-length-selector">
                                                <c:forEach items="${model.textLengths}" var="textLength">
                                                    <option value="${textLength.value}">${textLength.key}</option>
                                                </c:forEach>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="loading-indicator" style="display: none;">
                                        <i class="fa fa-2x fa-spinner fa-pulse fa-fw"></i>
                                    </div>
                                </div>
                                <div class="col col-md-4 text-right">
                                    <button type="button" class="btn btn-secondary reset-history-button"
                                            title="${cpn:i18n(slingRequest,'Resets this form including the whole history.')}">
                                        <cpn:text i18n="true">Reset History</cpn:text>
                                    </button>
                                    <button type="button" class="btn btn-secondary back-button"
                                            title="${cpn:i18n(slingRequest,'You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}">
                                        <cpn:text i18n="true">Back</cpn:text>
                                    </button>
                                    <button type="button" class="btn btn-secondary forward-button"
                                            title="${cpn:i18n(slingRequest,'You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}">
                                        <cpn:text i18n="true">Forward</cpn:text>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="panel panel-default mb-3">
                        <div class="panel-heading"
                             title="${cpn:i18n(slingRequest,'The possible content the AI created. Please feel free to edit it as you like.')}">
                            <cpn:text i18n="true">Content Suggestion</cpn:text>
                        </div>
                        <div class="panel-body">
                            <div class="form-group">
                                <c:choose>
                                    <c:when test="${model.isRichText}">
                                        <cpp:widgetForm disabled="true"
                                                        cssBase="composum-pages-options-ai-dialogs-create"
                                                        cssAdd="ai-response-field">
                                            <cpp:widget type="richtext"
                                                        modelClass="com.composum.ai.composum.bundle.model.CreateDialogModel"/>
                                        </cpp:widgetForm>
                                    </c:when>
                                    <c:otherwise>
                                        <textarea name="response"
                                                  class="form-control ai-response-field" rows="10"></textarea>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary cancel-button" data-dismiss="modal"
                            title="${cpn:i18n(slingRequest,'Cancel')}">
                        <cpn:text i18n="true">Cancel</cpn:text>
                    </button>
                    <button type="button" class="btn btn-primary append-button" data-dismiss="modal"
                            title="${cpn:i18n(slingRequest,'Append the created text to the editor of the component property you were editing')}">
                        <cpn:text i18n="true">Append</cpn:text>
                    </button>
                    <button type="button" class="btn btn-primary replace-button" data-dismiss="modal"
                            title="${cpn:i18n(slingRequest,'Replace the content of the editor for the component property you were editing with the created text')}">
                        <cpn:text i18n="true">Replace</cpn:text>
                    </button>
                </div>
            </form>
        </div>
    </div>
</cpp:element>

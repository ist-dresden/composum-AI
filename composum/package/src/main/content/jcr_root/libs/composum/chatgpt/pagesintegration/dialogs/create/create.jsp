<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.chatgpt.bundle.model.ChatGPTCreateDialogModel"
             cssBase="composum-chatgpt-pagesintegration-dialogs-create"
             cssAdd="dialog modal fade composum-chatgpt-dialog" role="dialog">
    <%--@elvariable id="model" type="com.composum.chatgpt.bundle.model.ChatGPTCreateDialogModel"--%>
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
                        <div class="alert alert-danger alert-text" style="display: none;"></div>
                    </div>

                    <div class="panel panel-default mb-3">
                        <div class="panel-heading" title="${cpn:i18n(slingRequest,'Input Prompt')}">
                            <cpn:text i18n="true">Input Prompt</cpn:text>
                        </div>
                        <div class="panel-body">
                            <div class="form-row">
                                <div class="form-group col-md-4"
                                     title="${cpn:i18n(slingRequest,'This replaces the prompt by one of a number of predefined prompts you can use directly or use as an example for your own prompt.')}">
                                    <label for="predefinedPrompts"
                                           title="${cpn:i18n(slingRequest,'Predefined Prompts')}">
                                        <cpn:text i18n="true">Predefined Prompts</cpn:text>
                                    </label>
                                    <select id="predefinedPrompts" name="predefined"
                                            class="form-control predefined-prompts">
                                        <c:forEach items="${model.predefinedPrompts}" var="predefinedPrompt">
                                            <option value="${predefinedPrompt.value}">${predefinedPrompt.key}</option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div class="form-group col-md-4"
                                     title="${cpn:i18n(slingRequest,'Select which text the AI receives in addition to your prompt, if any.')}">
                                    <label for="contentSelector"
                                           title="${cpn:i18n(slingRequest,'Base Text')}">
                                        <cpn:text i18n="true">Base Text</cpn:text>
                                    </label>
                                    <select id="contentSelector" name="contentSelect"
                                            class="form-control content-selector">
                                        <c:forEach items="${model.contentSelectors}" var="contentSelector">
                                            <option value="${contentSelector.key}">${contentSelector.value}</option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div class="form-group col-md-4">
                                    <label for="textLengthSelector"
                                           title="${cpn:i18n(slingRequest,'The desired approximate length of the generated text.')}">
                                        <cpn:text i18n="true">Text Length</cpn:text>
                                    </label>
                                    <select id="textLengthSelector" name="textLength"
                                            class="form-control text-length-selector">
                                        <c:forEach items="${model.textLengths}" var="textLength">
                                            <option value="${textLength.value}">${textLength.key}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>

                            <div class="form-group">
                                <label for="promptTextarea"
                                       title="${cpn:i18n(slingRequest,'Your request to the AI. If you like some examples you can select one of the predefined prompts - that will replace the content of this field.')}">
                                    <cpn:text i18n="true">Input Prompt</cpn:text>
                                </label>
                                <textarea id="promptTextarea" name="prompt" class="form-control prompt-textarea"
                                          rows="5"></textarea>
                            </div>

                            <div class="row align-items-center">
                                <div class="col-md-6 generate-container">
                                    <button type="button" class="btn btn-primary generate-button"
                                            title="${cpn:i18n(slingRequest,'Triggers the text generation - please give that a couple of seconds.')}">
                                        <cpn:text i18n="true">Generate</cpn:text>
                                    </button>
                                    <div class="loading-indicator" style="display: none;">
                                        <i class="fa fa-2x fa-spinner fa-pulse fa-fw"></i>
                                    </div>
                                </div>
                                <div class="col-md-6 text-right">
                                    <button type="button" class="btn btn-secondary reset-button"
                                            title="${cpn:i18n(slingRequest,'Resets this form.')}">
                                        <cpn:text i18n="true">Reset</cpn:text>
                                    </button>
                                    <span title="${cpn:i18n(slingRequest,'You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}">
                                    <button type="button" class="btn btn-secondary back-button">
                                        <cpn:text i18n="true">Back</cpn:text>
                                    </button>
                                    <button type="button" class="btn btn-secondary forward-button">
                                        <cpn:text i18n="true">Forward</cpn:text>
                                    </button>
                                    </span>
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
                                <textarea name="response"
                                          class="form-control chatgpt-response-field" rows="10"></textarea>
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

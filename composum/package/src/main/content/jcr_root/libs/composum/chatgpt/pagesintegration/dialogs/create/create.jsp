<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.chatgpt.bundle.model.ChatGPTCreateDialogModel"
             cssBase="composum-chatgpt-pagesintegration-dialogs-create"
             cssAdd="dialog modal fade" role="dialog">
    <%--@elvariable id="model" type="com.composum.chatgpt.bundle.model.ChatGPTCreateDialogModel"--%>
    <div class="modal-dialog form-panel">
        <div class="modal-content">
            <form class="widget-form">
                <input name="_charset_" type="hidden" value="UTF-8"/>

                <div class="modal-header">
                    <button type="button" class="close fa fa-close" data-dismiss="modal" aria-label="Close"></button>
                    <cpn:text tagName="h4" class="modal-title dialog_title text"
                              i18n="true">Content creation assistant</cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="alert alert-danger alert-text" style="display: none;"></div>
                    </div>

                    <div class="panel panel-default mb-3">re
                        <div class="panel-heading">Prompt</div>
                        <div class="panel-body">
                            <div class="form-row">
                                <div class="form-group col-md-4">
                                    <label for="predefinedPrompts">Predefined Prompts</label>
                                    <select id="predefinedPrompts" name="predefined"
                                            class="form-control predefined-prompts">
                                        <c:forEach items="${model.predefinedPrompts}" var="predefinedPrompt">
                                            <option value="${predefinedPrompt.value}">${predefinedPrompt.key}</option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div class="form-group col-md-4">
                                    <label for="contentSelector">Base Content</label>
                                    <select id="contentSelector" name="contentSelect"
                                            class="form-control content-selector">
                                        <c:forEach items="${model.contentSelectors}" var="contentSelector">
                                            <option value="${contentSelector.key}">${contentSelector.value}</option>
                                        </c:forEach>
                                    </select>
                                </div>

                                <div class="form-group col-md-4">
                                    <label for="textLengthSelector">Text Length</label>
                                    <select id="textLengthSelector" name="textLength"
                                            class="form-control text-length-selector">
                                        <c:forEach items="${model.textLengths}" var="textLength">
                                            <option value="${textLength.value}">${textLength.key}</option>
                                        </c:forEach>
                                    </select>
                                </div>
                            </div>

                            <div class="form-group">
                                <label for="promptTextarea">Prompt</label>
                                <textarea id="promptTextarea" name="prompt" class="form-control prompt-textarea"
                                          rows="5"></textarea>
                            </div>

                            <div class="row align-items-center">
                                <div class="col-md-6 generate-container">
                                    <button type="button" class="btn btn-primary generate-button">Generate</button>
                                    <div class="loading-indicator" style="display: none;"><i
                                            class="fa fa-2x fa-spinner fa-pulse fa-fw"></i></div>
                                </div>
                                <div class="col-md-6 text-right">
                                    <button type="button" class="btn btn-secondary back-button">Back</button>
                                    <button type="button" class="btn btn-secondary forward-button">Forward</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="panel panel-default mb-3">
                        <div class="panel-heading">Content suggestion</div>
                        <div class="panel-body">
                            <div class="form-group">
                                <textarea name="response"
                                          class="form-control chatgpt-response-field" rows="10" readonly></textarea>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary cancel-button" data-dismiss="modal">Cancel</button>
                    <button type="button" class="btn btn-primary append-button" data-dismiss="modal">Append</button>
                    <button type="button" class="btn btn-primary replace-button" data-dismiss="modal">Replace</button>
                </div>
            </form>
        </div>
    </div>
</cpp:element>

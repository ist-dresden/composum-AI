<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.chatgpt.bundle.model.ChatGPTTranslationDialogModel"
             cssBase="composum-chatgpt-pagesintegration-dialogs-translate"
             id="chatgpt-translate-dialog" cssAdd="dialog modal fade" role="dialog">
    <%--@elvariable id="model" type="com.composum.chatgpt.bundle.model.ChatGPTTranslationDialogModel"--%>
    <div class="modal-dialog form-panel">
        <div class="modal-content">
            <form class="widget-form">
                <input name="_charset_" type="hidden" value="UTF-8"/>
                <input name="path" type="hidden" value="${model.path}"/>
                <input name="property" type="hidden" value="${model.propertyName}"/>
                <input name="property" type="hidden" value="${model.fieldType}"/>

                <div class="modal-header">
                    <button type="button" class="close fa fa-close" data-dismiss="modal" aria-label="Close"></button>
                    <cpn:text tagName="h4" class="modal-title dialog_title text"
                              i18n="true">Translation Assistant</cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="alert"></div>
                    </div>

                    <div class="panel panel-default">
                        <div class="panel-heading" role="tab">
                            <cpn:text tagName="h4" class="panel-title dialog_title text"
                                      i18n="true">Source Language</cpn:text>
                        </div>
                        <div class="panel-body">
                            <div class="source-languages">
                                <c:forEach var="source" items="${model.sources}">
                                    <div class="source-language form-group">
                                        <div class="language-select">
                                            <label class="composum-pages-edit-widget_option">
                                                <input class="language-select-radio ${model.singleSourceClass}" type="radio" name="sourceLanguage"
                                                       value="${source.languageKey}">
                                                <span class="label-text">${source.languageName}:</span></label>
                                        </div>
                                        <cpn:text value="${source.text}" type="${model.fieldType}"/>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>

                    <div class="panel panel-default">
                        <div class="panel-heading" role="tab">
                            <cpn:text tagName="h4" class="panel-title dialog_title text"
                                      i18n="true">Translated text</cpn:text>
                        </div>
                        <div class="panel-body translations">
                            <div class="loading-curtain"><i class="fa fa-spinner fa-pulse fa-3x fa-fw"></i></div>
                            <div class="translation form-group" style="display: none;"></div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary accept">Accept</button>
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                </div>
            </form>
        </div>
    </div>
</cpp:element>

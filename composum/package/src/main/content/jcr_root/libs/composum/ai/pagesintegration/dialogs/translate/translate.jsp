<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.ai.composum.bundle.model.TranslationDialogModel"
             cssBase="composum-ai-pagesintegration-dialogs-translate"
             id="ai-translate-dialog" cssAdd="dialog modal fade composum-ai-dialog extra-wide" role="dialog">
    <%--@elvariable id="model" type="com.composum.ai.composum.bundle.model.TranslationDialogModel"--%>
    <div class="modal-dialog form-panel">
        <div class="modal-content">
            <form class="widget-form">
                <input name="_charset_" type="hidden" value="UTF-8"/>
                <input name="path" type="hidden" value="${model.path}"/>
                <input name="property" type="hidden" value="${model.propertyName}"/>
                <input name="property" type="hidden" value="${model.fieldType}"/>

                <div class="modal-header">
                    <sling:call script="buttons.jsp"/>
                    <cpn:text tagName="h4" class="modal-title dialog_title text" i18n="true">
                        Translation Assistant
                    </cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <cpn:text class="alert alert-warning truncationalert" i18n="true" style="display: none;">
                            The translation was too long and has been truncated.</cpn:text>
                        <div class="alert alert-warning generalalert" style="display: none;"></div>
                    </div>

                    <div class="panel panel-default"
                         title="${cpn:i18n(slingRequest,'This shows the source of the translation; if there are multiple source languages with texts for this property, you can select the source language here. Selecting a source language triggers the translation process.')}">
                        <div class="panel-heading" role="tab">
                            <cpn:text tagName="h4" class="panel-title dialog_title text" i18n="true">
                                Source Language
                            </cpn:text>
                        </div>
                        <div class="panel-body">
                            <div class="source-languages">
                                <c:forEach var="source" items="${model.sources}">
                                    <div class="source-language form-group">
                                        <div class="language-select">
                                            <label class="composum-pages-edit-widget_option">
                                                <input class="language-select-radio ${model.singleSourceClass}"
                                                       type="radio" name="sourceLanguage"
                                                       value="${source.languageKey}">
                                                <span class="label-text">${source.languageName}:</span></label>
                                        </div>
                                        <cpn:text value="${source.text}" type="${model.fieldType}"/>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>

                    <div class="panel panel-default"
                         title="${cpn:i18n(slingRequest,'This ')}">
                        <div class="panel-heading" role="tab">
                            <cpn:text tagName="h4" class="panel-title dialog_title text" i18n="true">
                                Translated text
                            </cpn:text>
                        </div>
                        <div class="panel-body translations">
                            <div class="loading-curtain" style="display: none;">
                                <i class="fa fa-spinner fa-pulse fa-3x fa-fw"
                                   title="${cpn:i18n(slingRequest,'Loading translation is in progress. Please wait a few seconds.')}">
                                </i>
                            </div>
                            <div class="translation form-group"></div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal"><cpn:text
                            i18n="true">Cancel</cpn:text></button>
                    <button type="button" class="btn btn-primary accept" data-dismiss="modal" disabled
                            title="${cpn:i18n(slingRequest,'Replaces the text for the property in the component editor by the given translation.')}">
                        <cpn:text
                                i18n="true">Accept</cpn:text></button>
                </div>
            </form>
        </div>
    </div>
</cpp:element>

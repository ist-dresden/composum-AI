<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
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

                <div class="modal-header">
                    <button type="button" class="close fa fa-close" data-dismiss="modal" aria-label="Close"></button>
                    <cpn:text tagName="h4" class="modal-title dialog_title text"
                              i18n="true">Translation Assistant</cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="alert">path: ${model.path}</div>
                        <div class="alert">path: ${model.propertyEditHandle}</div>
                    </div>

                    <div class="panel panel-default">
                        <div class="panel-heading" role="tab">
                            <cpn:text tagName="h4" class="panel-title dialog_title text"
                                      i18n="true">Source Language</cpn:text>
                        </div>
                        <div class="panel-body">
                            <div class="source-languages">
                                <div class="source-language form-group">
                                    <div class="language-select">
                                        <label class="composum-pages-edit-widget_option">
                                            <input class="language-select-radio" type="radio" name="sourceLanguage"
                                                   value="en"
                                                   checked="checked">
                                            <span class="label-text">english:</span></label>
                                    </div>
                                    <p>This is the text to translate. It can actually be pretty long, longer,
                                        loooooonger, longest, expecially if it's from a richtext. Let's have a really
                                        really long text, to see what happens here. Probably we
                                        have to cut it off sometimes. Also, it can be <u>richtext</u> and <em>have</em>
                                        <i>HTML</i>.</p>
                                    <p>It can also have several paragraphs. Also long ones.</p>
                                </div>
                            </div>
                            <div class="source-languages">
                                <div class="source-language form-group">
                                    <div class="language-select">
                                        <label class="composum-pages-edit-widget_option">
                                            <input class="language-select-radio" type="radio" name="sourceLanguage" value="fr"
                                                   checked="checked">
                                            <span class="label-text">french:</span></label>
                                    </div>
                                    <p>Voici le texte à traduire</p>
                                </div>
                            </div>
                        </div>
                    </div>

                    <div class="panel panel-default">
                        <div class="panel-heading" role="tab">
                            <cpn:text tagName="h4" class="panel-title dialog_title text"
                                      i18n="true">Translated text</cpn:text>
                        </div>
                        <div class="panel-body translations">
                            <div class="translation form-group">
                                <p>This is the translation.</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary accept" data-dismiss="modal">Accept</button>
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">Cancel</button>
                </div>
            </form>
        </div>
    </div>
</cpp:element>

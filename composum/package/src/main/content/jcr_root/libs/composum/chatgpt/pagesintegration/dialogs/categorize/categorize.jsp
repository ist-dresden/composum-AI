<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.chatgpt.bundle.model.ChatGPTCategorizeDialogModel"
             cssBase="composum-chatgpt-pagesintegration-dialogs-categorize"
             id="chatgpt-categorize-dialog" cssAdd="dialog modal fade" role="dialog">
    <%--@elvariable id="model" type="com.composum.chatgpt.bundle.model.ChatGPTCategorizeDialogModel"--%>
    <div class="modal-dialog form-panel">
        <div class="modal-content">
            <form class="widget-form">
                <input name="_charset_" type="hidden" value="UTF-8"/>
                <input name="path" type="hidden" value="${model.path}"/>

                <div class="modal-header">
                    <button type="button" class="close fa fa-close" data-dismiss="modal"
                            title="${cpn:i18n(slingRequest,'Close')}"
                            aria-label="${cpn:i18n(slingRequest,'Close')}"></button>
                    <cpn:text tagName="h4" class="modal-title dialog_title text"
                              i18n="true">Page Category Suggestions</cpn:text>
                </div>
                <div class="modal-body">
                    <div class="messages">
                        <div class="alert" style="display: none;"></div>
                        <cpn:text class="text alert alert-light" i18n="true">
                            This dialog helps set or update page categories.
                            Review 'Current Categories' and consider 'Suggested Categories' from AI.
                            Select desired categories, deselect unwanted ones.
                            Click 'Accept' to use the changes, 'Cancel' to discard.</cpn:text>
                    </div>

                    <c:if test="${model.hasCurrentCategories}">
                        <div class="panel panel-default">
                            <div class="panel-heading" role="tab">
                                <cpn:text tagName="h4" class="panel-title dialog_title text"
                                          i18n="true">Current Categories</cpn:text>
                            </div>
                            <div class="panel-body">
                                <div class="current-categories categorylist">
                                    <c:forEach var="category" items="${model.currentCategories}">
                                        <div class="category-item form-group">
                                            <div class="category-select">
                                                <label class="composum-pages-edit-widget_option">
                                                    <input class="category-select-checkbox"
                                                           type="checkbox" name="currentCategories"
                                                           value="${cpn:attr(request, category, 0)}"
                                                           checked="checked">
                                                    <cpn:text classes="label-text" tagName="span"
                                                              value="${category}"/></label>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </div>
                        </div>
                    </c:if>


                    <div class="panel panel-default">
                        <div class="panel-heading" role="tab">
                            <cpn:text
                                    tagName="h4" class="panel-title dialog_title text"
                                    i18n="true">Suggested Categories</cpn:text>
                        </div>
                        <div class="panel-body suggestions categorylist">
                            <div class="loading-curtain">
                                <i class="fa fa-spinner fa-pulse fa-3x fa-fw"
                                   title="${cpn:i18n(slingRequest,'Loading the suggestion for the categories. That might take a couple of seconds.')}"></i>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-dismiss="modal">
                        <cpn:text i18n="true">Cancel</cpn:text>
                    </button>
                    <button type="button" class="btn btn-primary accept" data-dismiss="modal">
                        <cpn:text i18n="true">Accept</cpn:text>
                    </button>
                </div>
            </form>
        </div>
    </div>
</cpp:element>

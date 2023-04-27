<%@page session="false" pageEncoding="UTF-8" %>
<%-- Loaded from /libs/composum/pages/commons/widget/labelextension.jsp via PagesPlugin, to integrate buttons for ChatGPT into widgets --%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:model var="model" type="com.composum.chatgpt.bundle.model.ChatGPTLabelExtensionModel">
    <%--@elvariable id="model" type="com.composum.chatgpt.bundle.model.ChatGPTLabelExtensionModel"--%>
    <c:if test="${model.translateButtonVisible}"><span
            class="${widgetCSS}_chatgptaction widget-chatgptaction action-translate fa fa-language"></span></c:if>
    <c:if test="${model.contentCreationButtonVisible}"><span
            class="${widgetCSS}_chatgptaction widget-chatgptaction action-create fa fa-magic"></span></c:if>
    <c:if test="${model.pageCategoriesButtonVisible}"><span
            class="${widgetCSS}_chatgptaction widget-chatgptaction action-pagecategories fa fa-tags"></span></c:if>
</cpp:model>

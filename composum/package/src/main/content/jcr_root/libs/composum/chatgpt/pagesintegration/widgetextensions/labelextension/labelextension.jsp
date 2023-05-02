<%@page session="false" pageEncoding="UTF-8" %>
<%-- Loaded from /libs/composum/pages/commons/widget/labelextension.jsp via PagesPlugin, to integrate buttons for ChatGPT into widgets --%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpp:element var="model" type="com.composum.chatgpt.bundle.model.ChatGPTLabelExtensionModel" test="${model.visible}">
    <%--@elvariable id="model" type="com.composum.chatgpt.bundle.model.ChatGPTLabelExtensionModel"--%>
    <div class="labelextension-wrapper">
        <c:if test="${model.translateButtonVisible}"><span
                class="widget-chatgptaction action-translate fa fa-language"
                title="Create a translation with ChatGPT"
                data-path="${model.path}" data-property="${model.property}"
        ></span></c:if>
        <c:if test="${model.contentCreationButtonVisible}"><span
                class="widget-chatgptaction action-create fa fa-magic"
                title="Create content with ChatGPT"
                data-path="${model.path}" data-property="${model.property}"
        ></span></c:if>
        <c:if test="${model.pageCategoriesButtonVisible}"><span
                class="widget-chatgptaction action-pagecategories fa fa-tags"
                title="Suggest categories with ChatGPT"
                data-path="${model.path}" data-property="${model.property}"
        ></span></c:if>
    </div>
</cpp:element>

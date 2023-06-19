<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<cpn:component var="model" type="com.composum.ai.composum.bundle.model.ChatGPTCategorizeDialogModel">
    <%--@elvariable id="model" type="com.composum.ai.composum.bundle.model.ChatGPTCategorizeDialogModel"--%>
    <c:forEach var="category" items="${model.suggestedCategories}">
        <div class="category-item form-group">
            <div class="category-select">
                <label class="composum-pages-edit-widget_option">
                    <input class="category-select-checkbox"
                           type="checkbox" name="currentCategories"
                           value="${cpn:attr(request, category, 0)}">
                    <cpn:text classes="label-text" tagName="span"
                              value="${category}"/></label>
            </div>
        </div>
    </c:forEach>
</cpn:component>

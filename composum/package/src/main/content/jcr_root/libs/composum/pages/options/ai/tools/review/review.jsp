<%@ page import="static com.composum.pages.commons.PagesConstants.RA_STICKY_LOCALE" %>
<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpp:defineFrameObjects/>
<% request.setAttribute(RA_STICKY_LOCALE, request.getLocale()); // use editors locale %>
<cpp:element var="element" type="com.composum.pages.stage.model.edit.FrameElement" mode="none"
             cssBase="composum-ai-pagesintegration-tools-review" cssAdd="composum-pages-tools">
    This is the review, 2
</cpp:element>
<% request.removeAttribute(RA_STICKY_LOCALE); %>

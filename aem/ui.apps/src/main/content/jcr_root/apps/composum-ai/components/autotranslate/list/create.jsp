<%@ page import="com.composum.ai.aem.core.impl.autotranslate.AutoTranslateListModel" %>
<%@ page import="org.apache.sling.api.SlingHttpServletRequest" %>
<%@page session="false" pageEncoding="UTF-8" %>
<%-- Redirects to page displaying the run --%>
<%
    SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
    AutoTranslateListModel model = slingRequest.adaptTo(AutoTranslateListModel.class);
    String id = model.createRun().id;
    response.sendRedirect("run.html/" + id);
%>

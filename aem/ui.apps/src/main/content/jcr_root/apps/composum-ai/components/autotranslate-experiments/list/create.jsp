<%@ page import="com.composum.ai.aem.core.impl.autotranslate.AutoTranslateListModel" %>
<%@ page import="org.apache.sling.api.SlingHttpServletRequest" %>
<%@page session="false" pageEncoding="UTF-8" %>
<%-- Redirects to page displaying the run --%>
<%
    try {
        SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        AutoTranslateListModel model = slingRequest.adaptTo(AutoTranslateListModel.class);
        String id = model.createRun().id;
        response.sendRedirect("run.html/" + id);
    } catch (Exception e) {
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Error during translation</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body class="coral--light foundation-layout-util-maximized-alt">
<div class="foundation-layout-panel">
    <div class="foundation-layout-panel-bodywrapper spectrum-ready">
        <div class="foundation-layout-panel-body">
            <div class="foundation-layout-panel-content foundation-layout-form foundation-layout-form-mode-edit">
                <div class="cq-dialog-content-page">
                    <p>Error: <%= e.toString() %>
                    </p>
                    <pre>
                        <%
                            out.flush();
                            e.printStackTrace(response.getWriter());
                        %>
                    </pre>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
<%
    }
%>

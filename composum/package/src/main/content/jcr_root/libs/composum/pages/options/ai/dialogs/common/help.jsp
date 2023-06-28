<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<cpn:defineObjects/>
<%
    String helppage = resourceResolver.getResource(resource.getResourceType()).getChild("help/jcr:content").getPath();
%>
<cpp:include path="<%= helppage %>" mode="none"
             resourceType="${resource.resourceType}" replaceSelectors="helpDialog"/>

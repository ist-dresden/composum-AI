<%@page session="false" pageEncoding="UTF-8"
        import="static com.composum.pages.commons.PagesConstants.RA_STICKY_LOCALE" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<% request.setAttribute(RA_STICKY_LOCALE, request.getLocale()); // use editors locale %>
<cpp:defineFrameObjects/>
<cpp:model var="model" type="com.composum.pages.commons.model.Page">
    <%--@elvariable id="model" type="com.composum.pages.commons.model.Page"--%>
    <div class="dialog modal fade composum-ai-dialog composum-ai-pagesintegration-dialogs-help extra-wide in"
         role="dialog">
        <div class="modal-dialog form-panel">
            <div class="modal-content">
                <div class="modal-header">
                    <sling:call script="buttons.jsp"/>
                    <h4><cpn:text value="${model.title}"/></h4>
                </div>
                <div class="modal-body">
                    <cpp:include path="${resource.path}" mode="none"
                                 resourceType="composum/pages/stage/edit/tools/component/help/page"
                                 replaceSelectors="parsys"/>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary cancel-button" data-dismiss="modal"
                            title="${cpn:i18n(slingRequest,'Close the help window')}">
                        <cpn:text i18n="true">Dismiss</cpn:text>
                    </button>
                </div>
            </div>
        </div>
    </div>
</cpp:model>
<% request.removeAttribute(RA_STICKY_LOCALE); %>

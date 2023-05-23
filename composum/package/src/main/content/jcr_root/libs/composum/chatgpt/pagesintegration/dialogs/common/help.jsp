<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects/>
<div class="dialog modal fade composum-chatgpt-dialog composum-chatgpt-pagesintegration-dialogs-help in" role="dialog">
    <div class="modal-dialog form-panel">
        <div class="modal-content">
            <div class="modal-header">
                <button type="button" class="close framebutton fa fa-close" data-dismiss="modal"
                        title="${cpn:i18n(slingRequest,'Close the dialog')}">
                </button>
                <sling:call script="helptitle.jsp"/>
            </div>
            <div class="modal-body">
                <sling:call script="helptext.jsp"/>
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

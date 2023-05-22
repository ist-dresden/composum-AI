<%@page session="false" pageEncoding="UTF-8" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<cpn:defineObjects/>
<div class="button-group">
    <button type="button" class="close framebutton fa fa-close" data-dismiss="modal"
            title="${cpn:i18n(slingRequest,'Close the dialog')}">
    </button>
    <button type="button" class="maximize framebutton fa fa-window-maximize"
            title="${cpn:i18n(slingRequest,'Maximize the dialog size')}">
    </button>
    <button type="button" class="restore framebutton fa fa-window-restore"
            title="${cpn:i18n(slingRequest,'Restore the normal dialog size')}">
    </button>
    <button type="button" class="help framebutton fa fa-question"
            title="${cpn:i18n(slingRequest,'Display help window')}">
    </button>
</div>

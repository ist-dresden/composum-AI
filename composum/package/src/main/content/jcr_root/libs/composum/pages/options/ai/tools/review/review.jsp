<%@page session="false" pageEncoding="UTF-8" %>
<%@ page import="static com.composum.pages.commons.PagesConstants.RA_STICKY_LOCALE" %>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2" %>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0" %>
<%@taglib prefix="cpp" uri="http://sling.composum.com/cppl/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<cpp:defineFrameObjects/>
<% request.setAttribute(RA_STICKY_LOCALE, request.getLocale()); // use editors locale %>
<cpp:element var="review" type="com.composum.ai.composum.bundle.model.SidebarDialogModel" mode="none"
             cssBase="composum-pages-options-ai-tools-review" cssAdd="composum-pages-tools">
    <div class="composum-pages-tools_actions btn-toolbar text-center">
        <span class="${reviewCssBase}_dialog-title">Composum-AI</span>
        <div class="composum-pages-tools_left-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-play generate-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Triggers an AI reply - please give that a couple of seconds. You can also press enter.')}">
                </button>
                <button type="button"
                        class="fa fa-stop stop-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Aborts an AI reply')}">
                </button>
            </div>
        </div>
        <div class="composum-pages-tools_right-actions">
            <div class="composum-pages-tools_button-group btn-group btn-group-sm" role="group">
                <button type="button"
                        class="fa fa-trash-o reset-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Resets this form but keeps the history.')}"></button>
                <button type="button"
                        class="fa fa-step-backward back-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Go back in the history of this dialog. You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}"></button>
                <button type="button"
                        class="fa fa-step-forward forward-button composum-pages-tools_button btn btn-default"
                        title="${cpn:i18n(slingRequest,'Go forward in the history of this dialog. You can make multiple tries to generate content and switch back and forth in a history of the dialog settings and the AI generated texts.')}"></button>
                <button type="button"
                        class="fa fa-question help-button composum-pages-tools_button btn btn-default"
                        data-helpurl="${slingRequest.requestURI}${slingRequest.requestPathInfo.selectorString}.help.${slingRequest.requestPathInfo.extension}${slingRequest.requestPathInfo.suffix}"
                        title="${cpn:i18n(slingRequest,'Display help window')}"></button>
            </div>
        </div>
    </div>
    <div class="${versionsCssBase}_panel composum-pages-tools_panel">
        <div class="${versionsCssBase}_versions-head">
            <i class="${versionsCssBase}_selection-icon fa fa-chevron-right"></i>
            <div class="${versionsCssBase}_primary-selection">
                <div class="${versionsCssBase}_selection-name"></div>
                <div class="${versionsCssBase}_selection-time"></div>
            </div>
            <div class="${versionsCssBase}_secondary-selection">
                <div class="${versionsCssBase}_selection-name"></div>
                <div class="${versionsCssBase}_selection-time"></div>
            </div>
            <div class="${versionsCssBase}_display-controls">
                <input class="${versionsCssBase}_version-slider widget slider-widget" type="text"
                       data-slider-min="0" data-slider-max="100" data-slider-step="1" data-slider-value="0"/>
            </div>
            <div class="${versionsCssBase}_compare-controls">
                <div class="label" title="${cpn:i18n(slingRequest,'compare properties')}">
                    <span>${cpn:i18n(slingRequest,'compare:')}</span>
                    <select class="${versionsCssBase}_property-filter widget select-widget"
                            title="${cpn:i18n(slingRequest,'compare properties')}">
                        <option value="properties">${cpn:i18n(slingRequest,'all')}</option>
                        <option value="text">${cpn:i18n(slingRequest,'text')}</option>
                        <option value="i18n">${cpn:i18n(slingRequest,'i18n')}</option>
                    </select>
                </div>
                <div class="label" title="${cpn:i18n(slingRequest,'highlight differences')}">
                    <input class="${versionsCssBase}_option-highlight widget checkbox-widget" type="checkbox"
                           title="${cpn:i18n(slingRequest,'highlight differences')}"/>
                    <span>${cpn:i18n(slingRequest,'highlight')}</span>
                </div>
                <div class="label" title="${cpn:i18n(slingRequest,'show equal values')}">
                    <input class="${versionsCssBase}_option-equal widget checkbox-widget" type="checkbox"
                           title="${cpn:i18n(slingRequest,'show equal values')}"/>
                    <span>${cpn:i18n(slingRequest,'show equal')}</span>
                </div>
            </div>
        </div>
        <div class="${versionsCssBase}_content">
                <%-- <sling:call script="versionList.jsp"/> - load after init via Ajax --%>
        </div>
    </div>
</cpp:element>

<% request.removeAttribute(RA_STICKY_LOCALE); %>

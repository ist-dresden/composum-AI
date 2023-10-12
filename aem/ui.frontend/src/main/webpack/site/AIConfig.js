/** Access to the AI Configuration Servlet */

const AICONFIG_SERVLET = '/bin/cpm/ai/config';

const enabledServicesCache = new Map();

class AIConfig {

    ifEnabled(service, callbackIfEnabled) {
        console.log("AIConfig ifEnabled", service, callbackIfEnabled);
        const editorUrl = window.location.pathname;
        let contentURL = Granite.author && Granite.author.ContentFrame && Granite.author.ContentFrame.contentURL;
        // if contentURL is not set, we check whether there is an item= parameter in the document.location.search
        if (!contentURL) {
            const search = window.location.search;
            const itemParam = search && search.match(/item=([^&]*)/);
            if (itemParam) {
                contentURL = itemParam[1];
            }
        }
        let result = enabledServicesCache.get([editorUrl, contentURL]);
        if (result) {
            if (result[service]) {
                console.log("AIConfig ifEnabled cached and true", service, callbackIfEnabled);
                callbackIfEnabled();
            } else {
                console.log("AIConfig ifEnabled cached and false", service, callbackIfEnabled);
            }
        } else {
            $.ajax({
                url: Granite.HTTP.externalize(AICONFIG_SERVLET) + ".json" + contentURL,
                type: "GET",
                cache: false,
                data: {editorUrl: editorUrl},
                dataType: "json"
            }).done(data => {
                console.log("AIConfig ifEnabled ajaxSuccess", service, callbackIfEnabled, data);
                if (data && data.allowedServices) {
                    result = data.allowedservices;
                    enabledServicesCache.set([editorUrl, contentURL], result);
                    if (result[service]) {
                        callbackIfEnabled();
                    }
                } else {
                    console.error("AIConfig: Unexpected response", data);
                    debugger;
                }
            }).fail((jqXHR, textStatus, errorThrown) => {
                console.error("AIConfig ajaxError", jqXHR, textStatus, errorThrown);
                debugger;
            });
        }

    }
}

export {AIConfig};

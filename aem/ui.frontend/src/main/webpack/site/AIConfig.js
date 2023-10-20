/** Access to the AI Configuration Servlet. */

const AICONFIG_SERVLET = '/bin/cpm/ai/config';

const enabledServicesCache = new Map();
const pendingCallsCache = new Map();

class AIConfig {

    getContentURL() {
        let contentURL = Granite?.author?.ContentFrame?.contentURL;
        // if contentURL is not set, we check whether there is an item= parameter in the document.location.search
        if (!contentURL) {
            const search = window.location.search;
            const itemParam = search?.match(/item=([^&]*)/);
            if (itemParam) {
                contentURL = itemParam[1];
            }
        }
        return contentURL;
    }

    /** Checks whether the named service is actually enabled for the current user, editor type and content URL. */
    ifEnabled(service, callbackIfEnabled) {
        // console.log("AIConfig ifEnabled", service);
        try {
            const editorUrl = window.location.pathname;
            let contentURL = this.getContentURL();
            const cachekey = editorUrl + "|||" + contentURL;
            const result = enabledServicesCache.get(cachekey);
            if (result) {
                if (result[service]) {
                    // console.log("AIConfig ifEnabled cached and true", service, cachekey);
                    callbackIfEnabled();
                } else {
                    // console.log("AIConfig ifEnabled cached and false", service, cachekey);
                }
            } else {
                const call = pendingCallsCache.get(cachekey) || $.ajax({
                    url: Granite.HTTP.externalize(AICONFIG_SERVLET) + ".json" + contentURL,
                    type: "GET",
                    cache: false,
                    data: {editorUrl: editorUrl},
                    dataType: "json"
                }).fail((jqXHR, textStatus, errorThrown) => {
                    console.error("AIConfig ajaxError", jqXHR, textStatus, errorThrown);
                    debugger;
                }).done(data => {
                    console.log("AIConfig ifEnabled ajaxSuccess", service, editorUrl, contentURL, data);
                    if (data?.allowedServices) {
                        enabledServicesCache.set(cachekey, data.allowedServices);
                    } else {
                        console.error("AIConfig: Unexpected response", data);
                        debugger;
                    }
                }).always(() => {
                    pendingCallsCache.delete(cachekey);
                });
                pendingCallsCache.set(cachekey, call);

                call.done(data => {
                    if (data?.allowedServices) {
                        const allowed = data.allowedServices[service];
                        // console.log("AIConfig ifEnabled allowed", service, cachekey, allowed);
                        if (allowed) {
                            callbackIfEnabled();
                        }
                    }
                });
            }
        } catch (e) { // rather catch all exceptions than break something outside
            console.error("AIConfig ifEnabled error", service, callbackIfEnabled, e);
            debugger;
        }
    }
}

export {
    AIConfig
};

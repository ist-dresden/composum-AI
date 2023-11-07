/** Access to the AI Configuration Servlet. */

const AICONFIG_SERVLET = '/bin/cpm/ai/config';

const enabledServicesCache = new Map();
const pendingCallsCache = new Map();

/**
 * Checks whether for the given response of the configuration service the service is enabled.
 * If the resourcetype isn't given, we just check whether the service is allowed *somewhere*.
 */
// data looks like this:
// {
//   "allowedServices": {
//     "sidepanel": true,
//     "create": true
//   },
//   "permissionInfo": {
//     "servicePermissions": [
//       {
//         "services": [
//           "create",
//           "sidepanel"
//         ],
//         "allowedComponents": [
//           ".*"
//         ],
//         "deniedComponents": []
//       }
//     ]
//   }
// }
function checkAllowed(data, service, resourceType) {
    // for all allowed.servicePermissions that contain the service, check whether the resourceType is allowed
    let result = false;
    const permissions = data?.permissionInfo?.servicePermissions;
    if (!permissions) {
        return result;
    }
    result = permissions.some(perm => {
        if (!perm.services.includes(service)) {
            return false;
        }

        if (!resourceType) {
            return true;
        }

        if (perm.deniedComponents.find(denied => resourceType.match(denied))) {
            return false;
        }

        return !!perm.allowedComponents.find(allowed => resourceType.match(allowed));
    });

    return result;
}

function getContentURL() {
    let contentURL = Granite?.author?.ContentFrame?.contentURL;
    // if contentURL is not set, we check whether there is an item= parameter in the document.location.search
    if (!contentURL) {
        const search = window.location.search;
        const itemParam = search?.match(/item=([^&]*)/);
        if (itemParam) {
            contentURL = itemParam[1];
        }
    } else if (contentURL.startsWith("/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html")) {
        // weird case in content fragment editor where that URL is just wrong. Remove that prefix.
        contentURL = contentURL.replace("/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html", "");
    }
    return contentURL;
}

class AIConfig {

    /** Checks whether the named service is actually enabled for the current user, editor type and content URL. */
    ifEnabled(service, resourceType, callbackIfEnabled) {
        // console.log("AIConfig ifEnabled", arguments);
        try {
            const editorUrl = window.location.pathname;
            let contentURL = getContentURL();
            const cachekey = editorUrl + "|||" + contentURL;
            const result = enabledServicesCache.get(cachekey);
            if (result) {
                if (checkAllowed(result, service, resourceType)) {
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
                    // console.log("AIConfig ifEnabled ajaxSuccess", service, editorUrl, contentURL, data);
                    if (data?.allowedServices) {
                        enabledServicesCache.set(cachekey, data);
                    } else {
                        console.error("AIConfig: Unexpected response", data);
                        debugger;
                    }
                }).always(() => {
                    pendingCallsCache.delete(cachekey);
                });
                pendingCallsCache.set(cachekey, call);

                call.done(data => {
                    if (checkAllowed(data, service, resourceType)) {
                        const allowed = data.allowedServices[service];
                        // console.log("AIConfig ifEnabled allowed", service, cachekey, allowed);
                        if (allowed) {
                            callbackIfEnabled();
                        }
                    } else {
                        // console.log("AIConfig ifEnabled not allowed", service, cachekey);
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

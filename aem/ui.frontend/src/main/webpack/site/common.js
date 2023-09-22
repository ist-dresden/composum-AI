// common functionality

/** A bit of heuristics to determine a sensible error text to display. */
function errorText(error) {
    var text;
    if (typeof error === 'string') {
        text = error;
    } else if (error.data && typeof error.data === 'string') {
        text = error.data;
    } else if (error.data) {
        text = JSON.stringify(error.data);
    } else {
        text = error instanceof Error ? error.message : JSON.stringify(error);
    }
    return text;
}

/** Find out the path for an edited content fragment, or undefined if it isn't one. */
function contentFragmentPath() {
    var url = Granite.author.ContentFrame.contentURL;
    url = url && url.toString();
    // e.g. ''/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html/content/dam/wknd/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc?tab=sidepanel-tab-structure'
    // we need the suffix, after .html, but check for .html/content/dam as marker
    // and we need to remove the query parameters
    var suffix;
    if (url && url.indexOf('.html/content/dam') >= 0) {
        suffix = url.substring(url.indexOf('.html/content/dam') + 5);
    }
    if (suffix && suffix.indexOf('?') >= 0) {
        suffix = suffix.substring(0, suffix.indexOf('?'));
    }
    return suffix;
}

export { errorText, contentFragmentPath };

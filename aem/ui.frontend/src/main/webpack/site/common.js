// common functionality

/** A bit of heuristics to determine a sensible error text to display. */
function errorText(error) {
    if (typeof error === 'string') {
        errorText = error;
    } else if (error.data && typeof error.data === 'string') {
        errorText = error.data;
    } else if (error.data) {
        errorText = JSON.stringify(error.data);
    } else {
        errorText = error instanceof Error ? error.message : JSON.stringify(error);
    }
    return errorText;
}

/** Find out the path for an edited content fragment, or undefined if it isn't one. */
function contentFragmentPath() {
    var url = Granite.author.ContentFrame.contentURL;
    url = url && url.toString();
    // e.g. '/mnt/overlay/dam/cfm/admin/content/v2/fragment-editor.html/content/dam/wknd/en/adventures/ski-touring-mont-blanc/ski-touring-mont-blanc'
    // we need the suffix, after .html, but check for .html/content/dam as marker
    var suffix;
    if (url && url.indexOf('.html/content/dam') >= 0) {
        suffix = url.substring(url.indexOf('.html/content/dam') + 5);
    }
    return suffix;
}

export { errorText, contentFragmentPath };

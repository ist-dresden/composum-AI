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

/** To make sure the dialogs have the right elements, we log an error and call the debugger if a mandatory element in a dialog is not found (Bug!) */
function findSingleElement($dialog, selector) {
    const $el = $dialog.find(selector);
    if ($el.length !== 1) {
        console.error('BUG! Dialog missing element for selector', $dialog.get(), selector, $el, $el.length);
        debugger;
    }
    return $el;
}

/**
 * Function to get and set the values of a single select coral-select.
 * There is a gaping functionality hole in AEM 6.5.7 about that.
 * @param $select the jQuery object for the coral-select (has to match exactly one element)
 * @param value the value to set, or undefined to get the current value
 */
// see https://www.danklco.com/posts/2017/12/coralui3-set-select-value.html
function coralSelectValue($select, value) {
    if (value === undefined) {
        return $select[0].values[0];
    } else {
        const select = $select[0];
        if (!select.items) {
            console.error('BUG! coral-select has no items', $select.get());
            debugger;
        } else {
            select.items.getAll().forEach(function (item, idx) {
                if (item.value === value) {
                    item.selected = true;
                }
            });
        }
    }
}

export {errorText, contentFragmentPath, findSingleElement, coralSelectValue};

/** Extensions for editor */

var waitingTimeout;
var retries;

/** Check whether the editor has an anchor #scrolltocomponent-path where path is an actual path like
 * /content/gfps/com/en/about-us/going-forward/jcr:content/content/heroautomaticdata
 * If it has, we remove that anchor from the URL and find the div with data-path with this path and scroll that into view.
 */
// e.g. http://localhost:5502/editor.html/content/gfps/com/en/about-us/going-forward.html#scrolltocomponent-content/container_copy_52774894/item_1510732628
function maybeScrollToComponent(event, args) {
    console.log('maybeScrollToComponent', event.type, event.target, args);
    Coral.commons.ready(document, function () {
        if (waitingTimeout) {
            clearTimeout(waitingTimeout);
        }
        retries = 40;
        waitingTimeout = setTimeout(scrollToComponent, 500);
    });
}

function scrollToComponent() {
    const url = new URL(window.location.href);
    const anchor = url.hash;
    url.hash = '';
    const newUrl = url.href;

    if (anchor.startsWith('#scrolltocomponent-')) {
        console.log('scrollToComponent', anchor);
        const path = Granite.author.page.path + '/jcr:content/' +
            anchor.replace('#scrolltocomponent-', '');
        const element = Granite?.author?.OverlayWrapper?.$el?.find(`div.cq-Overlay[data-path="${path}"]`);
        console.log('scrollToComponent', path, element.size(), element);
        if (element && element.size()) {
            setTimeout(() =>
                    Coral.commons.ready(Granite.author.OverlayWrapper.$el, () =>
                        element[0].scrollIntoView({behavior: 'smooth', block: 'start'})),
                500
            );
            setTimeout(() => // sometimes that doesn't work - retry :-(
                    Coral.commons.ready(Granite.author.OverlayWrapper.$el, () =>
                        element[0].scrollIntoView({behavior: 'smooth', block: 'start'})),
                2500
            );
            // Remove the anchor from the URL
            history.pushState({}, '', newUrl);
        } else if (retries > 0) {
            console.log('scrollToComponent retry', retries);
            retries--;
            waitingTimeout = setTimeout(scrollToComponent, 500);
        } else { // give up.
            history.pushState({}, '', newUrl);
        }
    }
}

export {maybeScrollToComponent};


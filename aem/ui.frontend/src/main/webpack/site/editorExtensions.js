/** Extensions for editor */

var waitingTimeout;

/** Check whether the editor has an anchor #scrolltocomponent-path where path is an actual path like
 * /content/gfps/com/en/about-us/going-forward/jcr:content/content/heroautomaticdata
 * If it has, we remove that anchor from the URL and find the div with data-path with this path and scroll that into view.
 */
// e.g. http://localhost:5502/editor.html/content/gfps/com/en/about-us/going-forward.html#scrolltocomponent-/content/gfps/com/en/about-us/going-forward/jcr:content/content/container_copy_52774894/item_1510732628
function maybeScrollToComponent(event, args) {
    console.log('maybeScrollToComponent', event.type, event.target, args);
    if (Granite?.author?.OverlayWrapper?.$el) {
        Coral.commons.ready(Granite.author.OverlayWrapper.$el, function () {
                if (waitingTimeout) {
                    clearTimeout(waitingTimeout);
                }
                waitingTimeout = setTimeout(scrollToComponent, 2000);
            }
        );
    }
}

function scrollToComponent() {
    const urlParams = new URL(window.location.href);
    const anchor = urlParams.hash;

    if (anchor.startsWith('#scrolltocomponent-')) {
        const path = anchor.replace('#scrolltocomponent-', '');
        // Remove the anchor from the URL
        // history.pushState({}, '', window.location.href.split('#')[0]);
        const element = Granite.author.OverlayWrapper.$el.find(`div[data-path="${path}"]`);
        debugger;
        if (element.size()) {
            element[0].scrollIntoView({block: 'center'});
        }
    }
}

export {maybeScrollToComponent};


/** Dictation service - a button records while pressed and then transcribes using the AIDictationServlet
 * and then inserts the result into a text area. */

const AIDICTATE_SERVLET = '/bin/cpm/ai/dictate';

class AIDictate {

    constructor(dictatebutton, textarea) {
        this.dictatebutton = $(dictatebutton);
        this.textarea = $(textarea);
        console.log("AIDictate constructor", this.dictatebutton, this.textarea);
        enable();
    }

    /** Performs a GET request to the servlet with current page path as suffix, and if that answers with 200 enable() is called. */
    enableCheck() {
        const url = Granite.HTTP.externalize(AIDICTATE_SERVLET) + ".txt/" + Granite.HTTP.getPath();
        fetch(url)
            .then(response => {
                if (response.status === 200) {
                    enable();
                }
            });
    }

    enable() {
        this.dictatebutton.removeClass('hide');
    }

}

export {AIDictate};

console.log("AIDictate.js loaded", AIDictate);

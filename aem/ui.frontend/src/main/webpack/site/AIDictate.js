/** Dictation service - a button records while pressed and then transcribes using the AIDictationServlet
 * and then inserts the result into a text area. */

import {AIConfig} from './AIConfig.js';

const AIDICTATE_SERVLET = '/bin/cpm/ai/dictate';

class AIDictate {

    constructor(dictatebutton, textarea) {
        this.dictatebutton = dictatebutton;
        this.textarea = textarea;
        console.log("AIDictate constructor", this.dictatebutton, this.textarea);
        this.enableCheck();
    }

    /** Performs a GET request to the servlet with current page path as suffix, and if that answers with 200 enable() is called. */
    enableCheck() {
        const contentUrl = new AIConfig().getContentURL();
        const url = Granite.HTTP.externalize(AIDICTATE_SERVLET) + ".txt" + contentUrl;
        fetch(url)
            .then(response => {
                if (response.status === 200) {
                    this.importRecorderAndEnable();
                }
            });
    }

    /** Adds a script https://cdnjs.cloudflare.com/ajax/libs/recorderjs/0.1.0/recorder.js to the document if Recorder isn't yet defined. */
    importRecorderAndEnable() {
        if (typeof Recorder === 'undefined') {
            const script = document.createElement('script');
            script.src = 'https://cdnjs.cloudflare.com/ajax/libs/recorderjs/0.1.0/recorder.js';
            script.onload = () => {
                this.enable();
            };
            document.head.appendChild(script);
        } else {
            this.enable();
        }
    }

    enable() {
        this.dictatebutton.removeClass('hide');
    }

}

export {AIDictate};

console.log("AIDictate.js loaded", AIDictate);

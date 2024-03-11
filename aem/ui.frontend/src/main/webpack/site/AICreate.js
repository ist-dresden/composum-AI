/** Content generation with AICreateServlet */

const AICREATE_SERVLET = '/bin/cpm/ai/create';

class AICreate {

    debug = true;
    verbose = false;

    constructor(streamingCallback, doneCallback, errorCallback) {
        this.streamingCallback = streamingCallback;
        this.doneCallback = doneCallback;
        this.errorCallback = errorCallback;
    }

    /** Aborts old calls and triggers a new call. */
    createContent(data) {
        if (this.debug) console.log("AICreate createContent", arguments);
        this.abortRunningCalls();

        this.runningxhr = new AbortController();
        const {signal} = this.runningxhr;

        // fetch call to AICreateServlet
        Granite.csrf.refreshToken().then(token => {
            const url = Granite.HTTP.externalize(AICREATE_SERVLET);
            if (!url.toString().includes('/bin/cpm/ai')) { // safety check since it's a POST request
                throw new Error('Invalid path: ' + url);
            }
            fetch(url, {
                method: "POST",
                cache: "no-cache",
                signal,
                headers: {'Content-Type': 'application/x-www-form-urlencoded', 'CSRF-Token': token},
                body: new URLSearchParams(data)
            })
                .then(response => {
                    if (this.debug) console.log("AICreate received response", response);
                    if (response.status === 200) {
                        this.runningxhr = undefined;
                        return response.json();
                    } else {
                        return response.text().then(errMsg => {
                            throw new Error("Unexpected response code " + response.status + " : " + errMsg);
                        });
                    }
                })
                .then(data => {
                    if (this.debug) console.log("AICreate received data", data);
                    const streamid = data.streamid;
                    if (streamid) {
                        this.startEventStream(streamid);
                    } else {
                        throw new Error("Bug: No streamid response " + JSON.stringify(data));
                    }
                })
                .catch(this.processError.bind(this));
        });
    }

    abortRunningCalls() {
        if (this.debug) console.log("AICreate abortRunningCalls", arguments);
        if (this.runningxhr) {
            this.runningxhr.abort();
            this.runningxhr = undefined;
        }
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = undefined;
        }
    }

    processError(error) {
        if (this.debug) console.log("AICreate ajaxError", arguments);
        debugger;
        this.runningxhr = undefined;
        const shortedError = error.toString().substring(0, 400);
        this.errorCallback(shortedError);
    }

    startEventStream(streamid) {
        if (this.debug) console.log("AICreate startEventStream", arguments);
        this.abortRunningCalls();
        this.streamingResult = "";
        this.eventSource = new EventSource(Granite.HTTP.externalize(AICREATE_SERVLET) + "?streamid=" + streamid);
        this.eventSource.onmessage = (event) => this.onStreamingMessage(this.eventSource, event);
        this.eventSource.onerror = (event) => this.onStreamingError(this.eventSource, event);
        this.eventSource.addEventListener('finished', (event) => this.onStreamingFinished(event));
        this.eventSource.addEventListener('exception', (event) => this.onStreamingException(event));
    }

    onStreamingMessage(eventSource, event) {
        if (this.verbose) console.log("AICreate onStreamingMessage", arguments);
        this.streamingResult += JSON.parse(event.data);
        this.streamingCallback(this.streamingResult);
    }

    onStreamingFinished(event) {
        if (this.debug) console.log("AICreate onStreamingFinished", arguments);
        this.doneCallback(this.streamingResult, JSON.parse(event.data));
        this.abortRunningCalls();
    }

    onStreamingError(eventSource, event) {
        if (this.debug) console.log("AICreate onStreamingError", arguments);
        this.errorCallback(event.data);
        this.abortRunningCalls();
    }

    onStreamingException(event) {
        if (this.debug) console.log("AICreate onStreamingException", arguments);
        this.errorCallback(event);
        this.abortRunningCalls();
    }

    dispose() {
        if (this.debug) console.log("AICreate dispose", arguments);
        this.abortRunningCalls();
    }

}

export {AICreate};

console.log("AICreate.js loaded", AICreate);

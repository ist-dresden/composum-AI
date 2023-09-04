/** Content generation with AICreateServlet */

const AICREATE_SERVLET = '/bin/cpm/ai/create';

class AICreate {
    constructor(streamingCallback, doneCallback, errorCallback) {
        this.streamingCallback = streamingCallback;
        this.doneCallback = doneCallback;
        this.errorCallback = errorCallback;
    }

    /** Aborts old calls and triggers a new call. */
    createContent(data) {
        this.abortRunningCalls();
        // ajax call to AICreateServlet
        this.runningxhr = $.ajax({
            url: Granite.HTTP.externalize(AICREATE_SERVLET),
            type: "POST",
            cache: false,
            data: data,
            success: this.ajaxSuccess.bind(this),
            error: this.ajaxError.bind(this)
        });
    }

    abortRunningCalls() {
        if (this.runningxhr) {
            this.runningxhr.abort();
            this.runningxhr = undefined;
        }
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = undefined;
        }
    }

    ajaxSuccess(data, status, jqXHR) {
        console.log("AICreate ajaxSuccess", arguments);
        this.runningxhr = undefined;
        // the servlet returns a 202 with a Location-redirect to the actual content
        if (jqXHR.status === 202) {
            const location = jqXHR.getResponseHeader('Location');
            if (location) {
                this.startEventStream(location);
            } else {
                console.error("Bug: No Location header in 202 response", arguments);
                this.errorCallback("Bug: No Location header in 202 response");
            }
        } else {
            console.error("Bug: Unexpected response code. ", arguments);
            this.errorCallback("Bug: Unexpected response code " + jqXHR.status);
        }
    }

    ajaxError(jqXHR, status, error) {
        console.log("AICreate ajaxError", arguments);
        this.runningxhr = undefined;
        this.errorCallback(error);
    }

    startEventStream(location) {
        this.abortRunningCalls();
        this.streamingResult = "";
        this.eventSource = new EventSource(location);
        this.eventSource.onmessage = this.onStreamingMessage.bind(this, this.eventSource);
        this.eventSource.onerror = this.onStreamingError.bind(this, this.eventSource);
        this.eventSource.addEventListener('finished', this.onStreamingFinished.bind(this));
        this.eventSource.addEventListener('exception', this.onStreamingException.bind(this));
    }

    onStreamingMessage(eventSource, event) {
        console.log("AICreate onStreamingMessage", arguments);
        this.streamingResult += JSON.parse(event.data);
        this.streamingCallback(this.streamingResult);
    }

    onStreamingFinished(event) {
        console.log("AICreate onStreamingFinished", arguments);
        this.doneCallback(this.streamingResult, JSON.parse(event.data));
        this.abortRunningCalls();
    }

    onStreamingError(eventSource, event) {
        console.log("AICreate onStreamingError", arguments);
        this.errorCallback(event.data);
        this.abortRunningCalls();
    }

    onStreamingException(event) {
        console.log("AICreate onStreamingException", arguments);
        this.errorCallback(event);
        this.abortRunningCalls();
    }

    dispose() {
        this.abortRunningCalls();
    }

}

export {AICreate};

console.log("AICreate.js loaded", AICreate);

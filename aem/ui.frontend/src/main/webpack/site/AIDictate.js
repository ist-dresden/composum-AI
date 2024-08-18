/** Dictation service - a button records while pressed and then transcribes using the AIDictationServlet
 * and then inserts the result into a text area. */

import {AIConfig} from './AIConfig.js';

const AIDICTATE_SERVLET = '/bin/cpm/ai/dictate';

class AIDictate {

    /**
     * Creates a new AIDictate instance.
     * @param {string} dictatebutton - Selector for the button that starts and stops recording.
     * @param {string} textarea - Selector for the textarea where the transcription is inserted.
     * @param {function} onChangeCallback - Callback function that is called after the transcription is inserted.
     */
    constructor(dictatebutton, textarea, onChangeCallback, onErrorCallback) {
        this.dictatebutton = $(dictatebutton)[0];
        this.onChangeCallback = onChangeCallback;
        this.setTextarea(textarea);
        this.recorder = null;
        this.audioStream = null;
        this.timeoutCall = null;
        this.isRecording = false;
        this.isStoppingRecording = false;
        this.dictateUrl = Granite.HTTP.externalize(AIDICTATE_SERVLET) + ".txt" + new AIConfig().getContentURL();
        console.log("AIDictate constructor", this.dictatebutton, this.textarea);
        this.enableCheck();
        this.attachEventListeners();
    }

    setTextarea(textarea) {
        this.textarea = $(textarea)[0];
        // verify that this is a textarea or text input field
        if (!this.textarea || !this.textarea.tagName || !this.textarea.tagName.match(/textarea|input/i)) {
            throw new Error('AIDictate: textarea parameter must be a textarea or text input field');
        }
        this.lastPosition = this.textarea.selectionStart;
    }

    /** Performs a GET request to the servlet with current page path as suffix,
     *  and if that answers with 200 enable() is called. */
    enableCheck() {
        fetch(this.dictateUrl)
            .then(response => {
                if (response.status === 200) {
                    this.importRecorderAndEnable();
                }
            });
    }

    /** Adds a script https://cdnjs.cloudflare.com/ajax/libs/recorderjs/0.1.0/recorder.js to the document
     * if Recorder isn't yet defined. */
    importRecorderAndEnable() {
        if (typeof Recorder === 'undefined') {
            const script = document.createElement('script');
            script.src = 'https://cdnjs.cloudflare.com/ajax/libs/recorderjs/0.1.0/recorder.js';
            script.onload = () => {
                this.enable();
            };
            document.body.appendChild(script);
        } else {
            this.enable();
        }
    }

    enable() {
        this.dictatebutton.classList.remove('hide');
    }

    startRecording = async () => {
        if (!this.isRecording && !this.isStoppingRecording) {
            console.log('Recording...');
            this.audioStream = await navigator.mediaDevices.getUserMedia({audio: true});
            const audioContext = new AudioContext();
            const input = audioContext.createMediaStreamSource(this.audioStream);
            this.recorder = new Recorder(input, {numChannels: 1});
            this.recorder.record();
            this.timeoutCall = setTimeout(this.stopRecording, 120000); // Stop recording after 2 minutes
            this.isRecording = true;
        }
    };

    stopRecording = async () => {
        if (!this.isRecording || this.isStoppingRecording) {
            return;
        }
        this.isStoppingRecording = true;
        console.log('Stopping recording');
        this.dictatebutton.disabled = true;
        this.recorder.stop();
        clearTimeout(this.timeoutCall);
        this.audioStream.getTracks().forEach(track => track.stop());
        this.recorder.exportWAV(async (blob) => {
            Granite.csrf.refreshToken().then(token => {
                const promptText = this.textarea.value && this.textarea.selectionStart &&
                    this.textarea.value.substring(0, this.textarea.selectionStart);
                console.log('Exported WAV');
                const formData = new FormData();
                formData.append('audioStream', blob);
                formData.append('contentType', 'audio/wav');
                formData.append('language', 'en'); // Replace with desired language code or dynamic value
                // Optionally append the prompt
                formData.append('prompt', promptText);

                console.log('Sending request');
                fetch(this.dictateUrl, {
                    method: 'POST',
                    body: formData,
                    headers: {'CSRF-Token': token}
                }).then(response => {
                    console.log('Received response', response.status);
                    if (response.ok) {
                        return response.text();
                    } else {
                        throw new Error(`Error: ${response.statusText}`);
                    }
                }).then(data => {
                    console.log('Received data', data);
                    this.insertResult(data.trim());
                }).catch(error => {
                    this.onError(error);
                    debugger;
                }).finally(() => {
                    console.log('Finished, enabling button');
                    this.dictatebutton.disabled = false;
                    this.recorder = null;
                    this.isRecording = false;
                    this.isStoppingRecording = false;
                });
            });
        });
    };

    insertResult(data) {
        // Insert transcription at current cursor position
        let cursorPosition = document.activeElement === this.textarea && this.textarea.selectionStart ?
            this.textarea.selectionStart : this.lastPosition;
        let value = this.textarea.value || '';
        const textBefore = value.substring(0, cursorPosition);
        const textAfter = value.substring(cursorPosition);
        this.textarea.value = `${textBefore}${/\s$/.test(textBefore) ? '' : ' '}${data}${/^\s/.test(textAfter) ? '' : ' '}${textAfter}`;
        this.textarea.selectionStart = cursorPosition + (/\s$/.test(textBefore) ? 0 : 1) + data.length + (/^\s/.test(textAfter) ? 0 : 1);
        this.textarea.selectionEnd = this.textarea.selectionStart;
        this.lastPosition = this.textarea.selectionStart;
        this.textarea.focus();
        this.textarea.scrollIntoViewIfNeeded();
        if (this.onChangeCallback) {
            this.onChangeCallback();
        }
    }

    attachEventListeners() {
        this.dictatebutton.addEventListener('mousedown', this.startRecording);
        this.dictatebutton.addEventListener('mouseup', this.stopRecording);
        window.addEventListener('keydown', (e) => {
            if (e.metaKey && e.ctrlKey && e.key === 't') {
                this.startRecording();
                e.preventDefault();
            }
        });
        window.addEventListener('keyup', () => {
            if (!this.isStoppingRecording) {
                this.stopRecording();
            }
        });
        this.textarea.addEventListener('blur', () => {
            this.lastPosition = this.textarea.selectionStart;
        });
    }

    onError(error) {
        console.error(error);
        if (this.onErrorCallback) {
            this.onErrorCallback(error);
        }
    }

}

export {AIDictate};

console.log("AIDictate.js loaded", AIDictate);

/** Dictation service - a button records while pressed and then transcribes using the AIDictationServlet
 * and then inserts the result into a text area. */

import {AIConfig} from './AIConfig.js';

const AIDICTATE_SERVLET = '/bin/cpm/ai/dictate';

class AIDictate {

    constructor(dictatebutton, textarea) {
        this.dictatebutton = $(dictatebutton)[0];
        this.textarea = $(textarea)[0];
        this.recorder = null;
        this.audioStream = null;
        this.timeoutCall = null;
        this.isRecording = false;
        this.isStoppingRecording = false;
        this.lastPosition = 0;
        this.dictateUrl = Granite.HTTP.externalize(AIDICTATE_SERVLET) + ".txt" + new AIConfig().getContentURL();
        console.log("AIDictate constructor", this.dictatebutton, this.textarea);
        this.enableCheck();
        this.attachEventListeners();
    }

    /** Performs a GET request to the servlet with current page path as suffix, and if that answers with 200 enable() is called. */
    enableCheck() {
        fetch(this.dictateUrl)
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
            this.audioStream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const audioContext = new AudioContext();
            const input = audioContext.createMediaStreamSource(this.audioStream);
            this.recorder = new Recorder(input, { numChannels: 1 });
            this.recorder.record();
            this.timeoutCall = setTimeout(this.stopRecording, 120000); // Stop recording after 2 minutes
            this.isRecording = true;
        }
    };

    stopRecording = async () => {
        if (!this.isRecording || this.isStoppingRecording) return;
        this.isStoppingRecording = true;
        console.log('Stopping recording');
        this.dictatebutton.disabled = true;
        this.recorder.stop();
        clearTimeout(this.timeoutCall);
        this.audioStream.getTracks().forEach(track => track.stop());
        this.recorder.exportWAV(async (blob) => {
            console.log('Exported WAV');
            const formData = new FormData();
            formData.append('audioStream', blob);
            formData.append('contentType', 'audio/wav');
            formData.append('language', 'en'); // Replace with desired language code or dynamic value
            // Optionally append the prompt
            const promptText = this.textarea.value.substring(0, this.textarea.selectionStart);
            formData.append('prompt', promptText);

            Granite.csrf.refreshToken().then(token => {
                console.log('Sending request');
                fetch(this.dictateUrl, {
                    method: 'POST',
                    body: formData,
                    headers: { 'CSRF-Token': token }
                } ).then(response => {
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
                    alert(`Error: ${error.message}`);
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
        let cursorPosition = document.activeElement === this.textarea ? this.textarea.selectionStart : this.lastPosition;
        const textBefore = this.textarea.value.substring(0, cursorPosition);
        const textAfter = this.textarea.value.substring(cursorPosition);
        this.textarea.value = `${textBefore}${/\s$/.test(textBefore) ? '' : ' '}${data}${/^\s/.test(textAfter) ? '' : ' '}${textAfter}`;
        this.textarea.selectionStart = cursorPosition + (/\s$/.test(textBefore) ? 0 : 1) + data.length + (/^\s/.test(textAfter) ? 0 : 1);
        this.textarea.selectionEnd = this.textarea.selectionStart;
        this.lastPosition = this.textarea.selectionStart;
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

}

export { AIDictate };

console.log("AIDictate.js loaded", AIDictate);

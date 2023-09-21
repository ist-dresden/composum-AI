/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';

const APPROXIMATE_MARKDOWN_SERVLET = '/bin/cpm/ai/approximated.markdown.md';

/**
 * Represents the Content Creation Dialog.
 */

class ContentCreationDialog {

    /**
     * Creates a new ContentCreationDialog.
     *
     * @param {Object} options - The options for the dialog.
     * @param {HTMLElement} options.dialog - The dialog element.
     * @param {string} options.componentPath - The path of the edited component.
     * @param {string} options.oldContent - The current content of the field.
     * @param {Function} options.writebackCallback - A function that takes the new content and writes it back to the field.
     * @param {boolean} options.isrichtext - True if the field is a richtext field, false if it's a plain text field.
     * @param {boolean} options.stackeddialog - True if the dialog is stacked and we have to close the dialog ourselves without generating events to not disturb the underlying dialog.
     * @param {Function} [options.onFinishCallback] - A function that is called when the dialog is closed.
     */
    constructor({dialog, componentPath, oldContent, writebackCallback, isrichtext, stackeddialog, onFinishCallback}) {
        console.log("ContentCreationDialog constructor ", arguments);
        this.componentPath = componentPath;
        this.$dialog = $(dialog);
        this.oldContent = oldContent;
        this.writebackCallback = writebackCallback;
        this.onFinishCallback = onFinishCallback;
        this.isrichtext = isrichtext;
        this.stackeddialog = stackeddialog;
        this.removeFormAction();
        this.assignElements();
        this.bindActions();
        this.createServlet = new AICreate(this.streamingCallback.bind(this), this.doneCallback.bind(this), this.errorCallback.bind(this));
        this.showError();
        this.setLoading(false);
        this.fullscreen();
        this.onPromptChanged();
        setTimeout(() => this.setSourceContent(oldContent), 300); // delay because rte editor might not be ready.
    }

    fullscreen() {
        this.$dialog.find('form').addClass('_coral-Dialog--fullscreenTakeover');
        this.$dialog.find('coral-dialog-footer').children().appendTo(this.$dialog.find('coral-dialog-header div.cq-dialog-actions'));
    }

    removeFormAction() {
        // we handle the submit ourselves.
        let form = this.$dialog.find('form');
        form.removeAttr('action');
        form.removeAttr('method');
    }

    findSingleElement(selector) {
        const $el = this.$dialog.find(selector);
        if ($el.length !== 1) {
            debugger;
            console.error('BUG! ContentCreationDialog: missing element for selector', selector, $el, $el.length);
        }
        return $el;
    }

    assignElements() {
        this.$prompt = this.findSingleElement('.composum-ai-prompt-textarea');
        this.$predefinedPromptsSelector = this.findSingleElement('.composum-ai-predefined-prompts');
        this.$contentSelector = this.findSingleElement('.composum-ai-content-selector');
        this.$sourceContent = this.isrichtext ? this.getRte(this.findSingleElement('.composum-ai-source-richtext'))
            : this.findSingleElement('.composum-ai-source-plaintext');
        this.$textLengthSelector = this.findSingleElement('.composum-ai-text-length-selector');
        this.$response = this.isrichtext ? this.getRte(this.findSingleElement('.composum-ai-response-richtext'))
            : this.findSingleElement('.composum-ai-response-plaintext');
        this.$generateButton = this.findSingleElement('.composum-ai-generate-button');
    }

    bindActions() {
        this.$predefinedPromptsSelector.on('change', this.onPredefinedPromptsChanged.bind(this));
        this.$prompt.on('change', this.onPromptChanged.bind(this));
        this.$contentSelector.on('change', this.onContentSelectorChanged.bind(this));
        this.$sourceContent.on('change', this.onSourceContentChanged.bind(this));
        this.findSingleElement('.composum-ai-generate-button').on('click', this.onGenerateButtonClicked.bind(this));
        this.findSingleElement('.composum-ai-stop-button').on('click', function () {
            this.createServlet.abortRunningCalls();
            this.setLoading(false);
        }.bind(this));
        this.findSingleElement('.composum-ai-reset-button').on('click', function () {
            this.$prompt.val('');
            this.setSourceContent(this.oldContent);
            this.setResponse('');
            this.onPredefinedPromptsChanged();
            this.onContentSelectorChanged();
            this.onPromptChanged();
        }.bind(this));
        this.findSingleElement('.cq-dialog-submit').on('click', this.onSubmit.bind(this));
        this.findSingleElement('.cq-dialog-cancel').on('click', this.onCancel.bind(this));
    }

    onPredefinedPromptsChanged(event) {
        console.log("onPredefinedPromptsChanged", arguments);
        const prompt = this.$predefinedPromptsSelector.val();
        if (prompt !== '-') {
            this.$prompt.val(prompt);
            this.onPromptChanged();
        }
    }

    onPromptChanged() {
        console.log("onPromptChanged", arguments);
        this.$predefinedPromptsSelector.val('-');
        if (this.$prompt.val() && this.$prompt.val().trim().length > 0) {
            this.$generateButton.removeAttr('disabled');
        } else {
            this.$generateButton.attr('disabled', 'disabled');
        }
    }

    onContentSelectorChanged(event) {
        console.log("onContentSelectorChanged", arguments);
        // possible values widget, component, page, lastoutput, -
        const key = this.$contentSelector.val();
        switch (key) {
            case 'lastoutput':
                this.setSourceContent(this.getResponse());
                break;
            case 'widget':
                this.setSourceContent(this.oldContent);
                break;
            case 'component':
                this.retrieveValue(this.componentPath, (value) => this.setSourceContent(value));
                break;
            case 'page':
                this.retrieveValue(this.pagePath(this.componentPath), (value) => this.setSourceContent(value));
                break;
            case 'empty':
                this.setSourceContent('');
                break;
            case '-':
                break;
            default:
                console.error('BUG! ContentCreationDialog: unknown content selector value', key);
        }
    }

    getRte($element) {
        const rte = $element.find('.cq-RichText-editable').data('rteinstance');
        if (!rte) {
            debugger;
        }
        return rte;
    }

    setSourceContent(value) {
        this.isrichtext ? this.$sourceContent.setContent(value) : this.$sourceContent.val(value);
    }

    getSourceContent() {
        return this.isrichtext ? this.$sourceContent.getContent() : this.$sourceContent.val();
    }

    setResponse(value) {
        this.isrichtext ? this.$response.setContent(value) : this.$response.val(value);
    }

    getResponse() {
        return this.isrichtext ? this.$response.getContent() : this.$response.val();
    }

    onSourceContentChanged(event) {
        console.log("onSourceContentChanged", arguments);
        this.$contentSelector.val('-');
    }

    retrieveValue(path, callback) {
        $.ajax({
            url: Granite.HTTP.externalize(APPROXIMATE_MARKDOWN_SERVLET + path
                + "?richtext=" + this.isrichtext
            ),
            type: "GET",
            dataType: "text",
            success: function (data) {
                callback(data);
            }.bind(this),
            error: function (xhr, status, error) {
                console.log("error loading approximate markdown", xhr, status, error);
            }
        });

        // http://localhost:4502/bin/cpm/ai/approximated.markdown.md/content/wknd/us/en/magazine/_jcr_content
        // http://localhost:4502/bin/cpm/ai/approximated.markdown/content/wknd/language-masters/composum-ai-testpages/jcr:content?_=1693499009746
    }

    /** The path until the /jcr:content */
    pagePath(path) {
        if (path.lastIndexOf('/jcr:content') > 0) {
            return path.substring(0, path.lastIndexOf('/jcr:content') + '/jcr:content'.length);
        } else if (path.lastIndexOf('_jcr_content') > 0) {
            return path.substring(0, path.lastIndexOf('_jcr_content') + '_jcr_content'.length);
        } else {
            return path;
        }
    }

    onGenerateButtonClicked(event) {
        console.log("onGenerateButtonClicked", arguments);
        this.showError(undefined);
        const data = {
            prompt: this.$prompt.val(),
            source: this.getSourceContent(),
            textLength: this.$textLengthSelector.val()
        };
        console.log("createContent", data);
        this.setLoading(true);
        this.createServlet.createContent(data);
        this.$dialog.find('.composum-ai-content-suggestion')[0].scrollIntoView();
    }

    streamingCallback(text) {
        console.log("ContentCreationDialog streamingCallback", arguments);
        this.setResponse(text);
    }

    doneCallback(text, event) {
        console.log("ContentCreationDialog doneCallback", arguments);
        this.streamingCallback(text);
        const finishreason = event && event.data && event.data.result && event.data.result.finishreason;
        if (finishreason === 'LENGTH') {
            this.showError('The generated content stopped because of the length restriction.');
        } else if (finishreason === 'STOP') {
            this.showError(undefined);
        } else {
            console.log("Unknown finishreason in ", event);
            this.showError("Internal error in text generation");
        }
        this.setLoading(false);
    }

    errorCallback(data) {
        console.log("ContentCreationDialog errorCallback", arguments);
        this.showError(data);
        this.setLoading(false);
    }

    setLoading(loading) {
        if (loading) {
            this.findSingleElement('.composum-ai-generate-button').attr('disabled', 'disabled');
            this.findSingleElement('.composum-ai-loading').show();
        } else {
            this.findSingleElement('.composum-ai-generate-button').removeAttr('disabled');
            this.findSingleElement('.composum-ai-loading').hide();
        }
    }

    /** Shows the error text if error is given, hides it if it's falsy. */
    showError(error) {
        if (!error) {
            this.findSingleElement('.composum-ai-error-columns').hide();
        } else {
            debugger;
            console.error("ContentCreationDialog showError", arguments);
            this.findSingleElement('.composum-ai-alert').text(error);
            this.findSingleElement('.composum-ai-error-columns').show();
        }
    }

    onSubmit(event) {
        console.log("ContentCreationDialog onSubmit", arguments);
        const response = this.getResponse();
        this.closeDialog(event);
        // only after closing since dialog is now out of the way
        if (typeof this.writebackCallback == 'function') {
            this.writebackCallback(response);
        }
    }

    onCancel(event) {
        console.log("ContentCreationDialog onCancel", arguments);
        this.closeDialog(event);
    }

    closeDialog(event) {
        if (this.stackeddialog) {
            // unfortunately otherwise the dialog closes the other dialog which we have been called from, too.
            event.preventDefault();
            event.stopPropagation();
            console.log('removing dialog', this.$dialog[0]);
            this.$dialog[0].remove();
        }
        // else: let the dialog close itself.
        if (typeof this.onFinishCallback == 'function') {
            this.onFinishCallback();
        }
    }

}

export {ContentCreationDialog};

console.log("ContentCreationDialog.js loaded", ContentCreationDialog);

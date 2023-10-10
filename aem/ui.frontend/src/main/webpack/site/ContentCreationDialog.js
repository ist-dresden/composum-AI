/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';
import {errorText, findSingleElement} from './common.js';
import {DialogHistory} from './DialogHistory.js';

const APPROXIMATE_MARKDOWN_SERVLET = '/bin/cpm/ai/approximated.markdown.md';

/** Keeps dialog histories per path. */
var historyMap = {};

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
     * @param {string} options.property - The name of the edited property.
     * @param {string} options.oldContent - The current content of the field.
     * @param {Function} options.writebackCallback - A function that takes the new content and writes it back to the field.
     * @param {boolean} options.isrichtext - True if the field is a richtext field, false if it's a plain text field.
     * @param {boolean} options.stackeddialog - True if the dialog is stacked and we have to close the dialog ourselves without generating events to not disturb the underlying dialog.
     * @param {Function} [options.onFinishCallback] - A function that is called when the dialog is closed.
     */
    constructor({dialog, componentPath, property, oldContent, writebackCallback, isrichtext, stackeddialog, onFinishCallback}) {
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
        const historyPath = property ? componentPath + '/' + property : componentPath;
        if (!historyMap[historyPath]) {
            historyMap[historyPath] = [];
        }
        this.history = new DialogHistory(this.$dialog, () => this.getDialogStatus(), (status) => this.setDialogStatus(status), historyMap[historyPath]);

        this.showError();
        this.setLoading(false);
        this.fullscreen();
        this.onPromptChanged();
        setTimeout(() => {
            this.setSourceContent(oldContent);
            this.history.restoreFromLastOfHistory();
        }, 300); // delay because rte editor might not be ready.
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

    assignElements() {
        this.$prompt = findSingleElement(this.$dialog, '.composum-ai-prompt-textarea');
        this.$predefinedPromptsSelector = findSingleElement(this.$dialog, '.composum-ai-predefined-prompts');
        this.$contentSelector = findSingleElement(this.$dialog, '.composum-ai-content-selector');
        this.$sourceContent = this.isrichtext ? this.getRte(findSingleElement(this.$dialog, '.composum-ai-source-richtext'))
            : findSingleElement(this.$dialog, '.composum-ai-source-plaintext');
        this.$textLengthSelector = findSingleElement(this.$dialog, '.composum-ai-text-length-selector');
        this.$response = this.isrichtext ? this.getRte(findSingleElement(this.$dialog, '.composum-ai-response-richtext'))
            : findSingleElement(this.$dialog, '.composum-ai-response-plaintext');
        this.$generateButton = findSingleElement(this.$dialog, '.composum-ai-generate-button');
    }

    getDialogStatus() {
        return {
            prompt: this.$prompt.val(),
            source: this.getSourceContent(),
            textLength: this.$textLengthSelector.val(),
            contentSelector: this.$contentSelector.val(),
            predefinedPrompts: this.$predefinedPromptsSelector.val(),
            response: this.getResponse()
        };
    }

    setDialogStatus(status) {
        this.$predefinedPromptsSelector.val(status.predefinedPrompts);
        this.$contentSelector.val(status.contentSelector);
        this.$textLengthSelector.val(status.textLength);
        this.$prompt.val(status.prompt);
        this.setSourceContent(status.source);
        this.setResponse(status.response);
    }

    bindActions() {
        this.$predefinedPromptsSelector.on('change', this.onPredefinedPromptsChanged.bind(this));
        this.$prompt.on('change input', this.onPromptChanged.bind(this));
        this.$contentSelector.on('change', this.onContentSelectorChanged.bind(this));
        this.$sourceContent.on('change', this.onSourceContentChanged.bind(this));
        findSingleElement(this.$dialog, '.composum-ai-generate-button').on('click', this.onGenerateButtonClicked.bind(this));
        findSingleElement(this.$dialog, '.composum-ai-stop-button').on('click', () => {
            this.createServlet.abortRunningCalls();
            this.setLoading(false);
            this.history.maybeSaveToHistory();
        });
        findSingleElement(this.$dialog, '.composum-ai-reset-button').on('click', () => {
            this.history.maybeSaveToHistory();
            this.$prompt.val('');
            this.setSourceContent(this.oldContent);
            this.setResponse('');
            this.onPredefinedPromptsChanged();
            this.onContentSelectorChanged();
            this.onPromptChanged();
        });
        findSingleElement(this.$dialog, '.cq-dialog-submit').on('click', this.onSubmit.bind(this));
        findSingleElement(this.$dialog, '.cq-dialog-cancel').on('click', this.onCancel.bind(this));
        this.$prompt.on('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey && !event.altKey && !event.metaKey) {
                event.preventDefault();
                this.onGenerateButtonClicked(event);
            }
        });
    }

    onPredefinedPromptsChanged(event) {
        console.log("onPredefinedPromptsChanged", arguments);
        const prompt = this.$predefinedPromptsSelector.val();
        if (prompt !== '-') {
            // this.history.maybeSaveToHistory(); // debatable: doesn't make sense if user just skips through the list.
            this.$prompt.val(prompt);
            this.onPromptChanged();
        }
    }

    onPromptChanged() {
        // console.log("onPromptChanged", arguments); // on every keypress
        this.$predefinedPromptsSelector.val('-');
        if (this.$prompt.val() && this.$prompt.val().trim().length > 0) {
            this.$generateButton.removeAttr('disabled');
        } else {
            this.$generateButton.attr('disabled', 'disabled');
        }
    }

    onContentSelectorChanged(event) {
        console.log("onContentSelectorChanged", arguments);
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
                this.showError('Unknown content selector value ' + key);
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
            success: (data) => {
                callback(data);
            },
            error: (xhr, status, error) => {
                console.error("error loading approximate markdown", xhr, status, error);
                this.showError(errorText(status + " " + error));
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
        // console.log("ContentCreationDialog streamingCallback", arguments);
        this.setResponse(text);
    }

    doneCallback(text, event) {
        this.streamingCallback(text);
        console.log("ContentCreationDialog doneCallback", arguments);
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
        this.history.maybeSaveToHistory();
    }

    errorCallback(data) {
        console.log("ContentCreationDialog errorCallback", arguments);
        this.showError(data);
        this.setLoading(false);
    }

    setLoading(loading) {
        if (loading) {
            findSingleElement(this.$dialog, '.composum-ai-generate-button').attr('disabled', 'disabled');
            findSingleElement(this.$dialog, '.composum-ai-loading').show();
        } else {
            findSingleElement(this.$dialog, '.composum-ai-generate-button').removeAttr('disabled');
            findSingleElement(this.$dialog, '.composum-ai-loading').hide();
        }
    }

    /** Shows the error text if error is given, hides it if it's falsy. */
    showError(error) {
        if (!error) {
            findSingleElement(this.$dialog, '.composum-ai-error-columns').hide();
        } else {
            console.error("ContentCreationDialog showError", arguments);
            findSingleElement(this.$dialog, '.composum-ai-alert').text(errorText(error));
            findSingleElement(this.$dialog, '.composum-ai-error-columns').show();
            debugger;
        }
    }

    /** Dialog submit: overwrite calling richtext editor / textarea in dialog */
    onSubmit(event) {
        console.log("ContentCreationDialog onSubmit", arguments);
        const response = this.getResponse();
        this.closeDialog(event);
        // only after closing since dialog is now out of the way
        if (typeof this.writebackCallback == 'function') {
            this.writebackCallback(response);
        }
    }

    /** Dialog cancel: just closes dialog. */
    onCancel(event) {
        console.log("ContentCreationDialog onCancel", arguments);
        this.closeDialog(event);
    }

    closeDialog(event) {
        console.log("ContentCreationDialog closeDialog", arguments);
        this.history.maybeSaveToHistory();
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

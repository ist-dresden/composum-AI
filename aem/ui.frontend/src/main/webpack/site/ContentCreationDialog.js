/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';
import {AIDictate} from './AIDictate.js';
import {errorText, findSingleElement} from './common.js';
import {DialogHistory} from './DialogHistory.js';
import {HelpPage} from './HelpPage.js';

const APPROXIMATED_MARKDOWN_SERVLET = '/bin/cpm/ai/approximated';

/** An array of maps with {prompt, contentSelector} maps containing the last prompts. */
const LOCALSTORAGE_KEY_CONTENTCREATION_PROMPTHISTORY = 'aem-composumAI-contentcreation-promptHistory';

const MAX_LAST_PROMPTS = 20;

/** Keeps dialog histories per path. */
const historyMap = {};

/**
 * Represents the Content Creation Dialog.
 */

class ContentCreationDialog {


    debug = true;

    /**
     * Creates a new ContentCreationDialog.
     *
     * @param {Object} options - The options for the dialog.
     * @param {HTMLElement} options.dialog - The dialog element.
     * @param {string} options.componentPath - The path of the edited component.
     * @param {string} options.property - The name of the edited property.
     * @param {string} options.oldContent - The current content of the field.
     * @param {Function} options.writebackCallback - A function that takes the new content and writes it back to the field.
     * @param {boolean} options.isRichtext - True if the field is a richtext field, false if it's a plain text field.
     * @param {boolean} options.stackeddialog - True if the dialog is stacked and we have to close the dialog ourselves without generating events to not disturb the underlying dialog.
     * @param {Function} [options.onFinishCallback] - A function that is called when the dialog is closed.
     */
    constructor({
                    dialog,
                    componentPath,
                    property,
                    oldContent,
                    writebackCallback,
                    isRichtext,
                    stackeddialog,
                    onFinishCallback
                }) {
        console.log("ContentCreationDialog constructor ", arguments);
        this.componentPath = componentPath;
        this.$dialog = $(dialog);
        this.oldContent = oldContent;
        this.writebackCallback = writebackCallback;
        this.onFinishCallback = onFinishCallback;
        this.isRichtext = isRichtext;
        this.stackeddialog = stackeddialog;
        this.removeFormAction();
        this.assignElements();
        this.bindActions();
        this.createServlet = new AICreate(this.streamingCallback.bind(this), this.doneCallback.bind(this), this.errorCallback.bind(this));
        this.dictate = new AIDictate(this.$dialog.find('.composum-ai-dictate-button'),
            this.$prompt, this.onPromptChanged.bind(this),
            this.showError.bind(this)
        );
        const historyPath = property ? componentPath + '/' + property : componentPath;
        if (!historyMap[historyPath]) {
            historyMap[historyPath] = [];
        }
        this.history = new DialogHistory(this.$dialog,
            () => this.getDialogStatus(),
            (status) => this.setDialogStatus(status),
            historyMap[historyPath]);
        this.lastPrompts = JSON.parse(localStorage.getItem(LOCALSTORAGE_KEY_CONTENTCREATION_PROMPTHISTORY)) || [];
        this.restoreLastPrompts();

        this.showError();
        this.setLoading(false);
        this.fullscreen();
        this.alignPromptAndSourceColumn();
        this.onPromptChanged();
        setTimeout(() => {
            this.setSourceContent(oldContent);
            this.history.restoreFromLastOfHistory();
        }, 300); // delay because rte editor might not be ready.
    }

    fullscreen() {
        this.$dialog.find('form').addClass('_coral-Dialog--fullscreenTakeover');
        this.$dialog.find('coral-dialog-footer').children().appendTo(this.$dialog.find('coral-dialog-header div.cq-dialog-actions'));
        this.$dialog.find('.composum-ai-prompt-columns .u-coral-padding').removeClass('u-coral-padding');
        // for AEM 6.5.7
        $('coral-dialog#composumAI-create-dialog').removeAttr('moveable').attr('fullscreen', 'fullscreen')
            .addClass('coral3-Dialog--fullscreen');
    }

    /* enlarge '.composum-ai-source-richtext [data-cq-richtext-editable=true]' so that div.composum-ai-source-column
     * and div.composum-ai-prompt-column have the same height
     * That's annoying to do with css, so we rather calculate and set the height here. */
    alignPromptAndSourceColumn() {
        const $editable = this.$dialog.find('.composum-ai-source-richtext [data-cq-richtext-editable=true]');
        const $sourceColumn = this.$dialog.find('.composum-ai-source-column section');
        const $promptColumn = this.$dialog.find('.composum-ai-prompt-column section');
        setTimeout(() => {
            const promptColumnScreenHeight = $promptColumn[0].clientHeight;
            $editable.css('height', '100px');
            const sourceColumnScreenHeight = $sourceColumn[0].clientHeight;
            const heightDiff = promptColumnScreenHeight - sourceColumnScreenHeight;
            if (heightDiff > 0) {
                $editable.css('height', (heightDiff + 100) + 'px');
            } else { // shouldn't happen - we rather leave it as it is.
                $editable.css('height', '');
            }
        }, 100); // make sure it's already visible
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
        this.$sourceContent = this.isRichtext ? this.getRte(findSingleElement(this.$dialog, '.composum-ai-source-richtext'))
            : findSingleElement(this.$dialog, '.composum-ai-source-plaintext');
        this.$textLengthSelector = findSingleElement(this.$dialog, '.composum-ai-text-length-selector');
        this.$lastPromptsSelector = findSingleElement(this.$dialog, '.composum-ai-last-prompt-selector');
        this.$response = this.isRichtext ? this.getRte(findSingleElement(this.$dialog, '.composum-ai-response-richtext'))
            : findSingleElement(this.$dialog, '.composum-ai-response-plaintext');
        this.$generateButton = findSingleElement(this.$dialog, '.composum-ai-generate-button');
        this.$stopButton = findSingleElement(this.$dialog, '.composum-ai-stop-button');
        this.$urlField = findSingleElement(this.$dialog, '.composum-ai-url-field');
        this.$urlContainer = this.$urlField.parent();
        this.$imageContainer = findSingleElement(this.$dialog, '.composum-ai-source-image-container');
        this.$image = findSingleElement(this.$imageContainer, '.composum-ai-source-image');
    }

    getDialogStatus() {
        const status = {
            prompt: this.$prompt.val(),
            source: this.getSourceContent(),
            imagepath: this.$image.data('imagepath'),
            textLength: this.$textLengthSelector.val(),
            contentSelector: this.$contentSelector.val(),
            predefinedPrompts: this.$predefinedPromptsSelector.val(),
            predefinedPromptsKey: this.$predefinedPromptsSelector.find('option:selected').text(),
            url: this.$urlField.val(),
            response: this.getResponse()
        };
        this.maybeStoreLastPrompt(status);
        return status;
    }

    setDialogStatus(status) {
        this.$predefinedPromptsSelector.val(status.predefinedPrompts);
        this.$contentSelector.val(status.contentSelector);
        this.$textLengthSelector.val(status.textLength);
        this.$prompt.val(status.prompt);
        if (status.source || status.imagepath) {
            this.setSourceContent(status.source, status.imagepath);
        } else {
            this.setSourceContent(this.oldContent);
        }
        this.setResponse(status.response);
        if (status.url) {
            this.$urlField.val(status.url);
        }
        this.onPredefinedPromptsChanged();
        this.onContentSelectorChanged();
        this.onPromptChanged();
    }

    bindActions() {
        this.$predefinedPromptsSelector.on('change', this.onPredefinedPromptsChanged.bind(this));
        this.$lastPromptsSelector.on('change', this.onLastPromptsChanged.bind(this));
        this.$prompt.on('change input', this.onPromptChanged.bind(this));
        this.$contentSelector.on('change', this.onContentSelectorChanged.bind(this));
        this.$sourceContent.on('change', this.onSourceContentChanged.bind(this));
        this.$urlField.on('change', this.onUrlChanged.bind(this));
        this.$urlField.on('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey && !event.altKey && !event.metaKey) {
                event.preventDefault();
                this.onUrlChanged(event);
            }
        });
        this.$generateButton.on('click', this.onGenerateButtonClicked.bind(this));
        findSingleElement(this.$dialog, '.composum-ai-stop-button').on('click', this.onStopClicked.bind(this));
        findSingleElement(this.$dialog, '.composum-ai-reset-button').on('click', this.resetForm.bind(this));
        findSingleElement(this.$dialog, '.cq-dialog-submit').on('click', this.onSubmit.bind(this));
        findSingleElement(this.$dialog, '.cq-dialog-cancel').on('click', this.onCancel.bind(this));
        this.$prompt.on('keydown', (event) => {
            if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey && !event.altKey && !event.metaKey) {
                event.preventDefault();
                this.onGenerateButtonClicked(event);
            }
        });
        findSingleElement(this.$dialog, '.cq-dialog-help').on('click', (event) => new HelpPage(event).show());
    }

    resetForm(event) {
        this.onStopClicked(event);
        this.setDialogStatus({});
    }

    onStopClicked(event) {
        if (this.debug) console.log("onStopClicked", arguments);
        this.createServlet.abortRunningCalls();
        this.setLoading(false);
        this.history.maybeSaveToHistory();
    }

    onPredefinedPromptsChanged(event) {
        if (this.debug) console.log("onPredefinedPromptsChanged", arguments);
        const predefinedPrompt = this.$predefinedPromptsSelector.val();
        if (predefinedPrompt !== '-') {
            // this.history.maybeSaveToHistory(); // debatable: doesn't make sense if user just skips through the list.
            this.$prompt.val(predefinedPrompt);
            this.$prompt[0].setSelectionRange(predefinedPrompt.length, predefinedPrompt.length);
            this.onPromptChanged();
        }
    }

    onPromptChanged() {
        if (this.debug) console.log("onPromptChanged", arguments); // on every keypress
        let prompt = this.$prompt.val();
        if (this.$predefinedPromptsSelector.val() !== prompt) {
            this.$predefinedPromptsSelector.val('-');
        }
        if (prompt && prompt.trim().length > 0) {
            this.$generateButton.removeAttr('disabled');
        } else {
            this.$generateButton.attr('disabled', 'disabled');
        }
        this.$lastPromptsSelector.val('');
        this.dictate.setTextarea(this.$prompt);
    }

    onContentSelectorChanged(event) {
        if (this.debug) console.log("onContentSelectorChanged", arguments);
        const key = this.$contentSelector.val();
        this.showUrl(false);
        this.$image.removeData('imagepath');
        switch (key) {
            case 'lastoutput':
                this.setSourceContent(this.getResponse());
                break;
            case 'widget':
                this.setSourceContent(this.oldContent);
                break;
            case 'component':
                this.retrieveValue(this.componentPath, this.setSourceContent.bind(this));
                break;
            case 'page':
                this.retrieveValue(this.pagePath(this.componentPath), this.setSourceContent.bind(this));
                break;
            case 'url':
                this.showError(false);
                this.$urlField.val('');
                this.onUrlChanged();
                this.showUrl(true);
                break;
            case 'empty':
                this.setSourceContent('');
                break;
            case '-':
                this.setSourceContent(''); // waiting for input
                break;
            default:
                if (key.startsWith('/content/')) {
                    this.retrieveValue(key, this.setSourceContent.bind(this));
                } else {
                    this.showError('Unknown content selector value ' + key);
                    debugger;
                }
        }
    }

    showUrl(urlvisible) {
        if (urlvisible) {
            this.$urlContainer.show();
        } else {
            this.$urlContainer.hide();
        }
        this.alignPromptAndSourceColumn();
    }

    onUrlChanged(event) {
        if (event && event.preventDefault && event.stopPropagation) {
            event.preventDefault();
            event.stopPropagation();
        }
        const url = this.$urlField.val();
        if (url && url.trim().length > 0) {
            console.log('fetching url ', url);
            $.ajax({
                url: Granite.HTTP.externalize(APPROXIMATED_MARKDOWN_SERVLET
                    + (this.isRichtext ? '.html' : '.md')
                    + '?fromurl=' + url.trim()
                ),
                type: "GET",
                dataType: "text",
                success: (data) => {
                    this.setSourceContent(data);
                },
                error: (xhr, status, error) => {
                    console.error("error loading approximate markdown for ", url, xhr, status, error);
                    this.showError(errorText(status + " " + error));
                }
            });
        }
    }

    maybeStoreLastPrompt(status) {
        if (this.debug) console.log("maybeStoreLastPrompt", arguments);
        const entry = {
            prompt: status.prompt,
            promptSelector: status.predefinedPrompts,
            promptSelectorKey: status.predefinedPromptsKey,
            contentSelector: status.contentSelector,
            url: status.url
        };
        if (this.debug) console.log("maybeStoreLastPrompt entry", entry);
        const entryString = JSON.stringify(entry);
        if (!this.lastPrompts || entryString !== JSON.stringify(this.lastPrompts[0])) {
            // delete entries that are the same as the new one
            this.lastPrompts = this.lastPrompts.filter((oldentry) => JSON.stringify(oldentry) !== entryString);
            this.lastPrompts.unshift(entry);
            if (this.lastPrompts.length > MAX_LAST_PROMPTS) {
                this.lastPrompts.pop();
            }
            localStorage.setItem(LOCALSTORAGE_KEY_CONTENTCREATION_PROMPTHISTORY, JSON.stringify(this.lastPrompts));
            this.restoreLastPrompts();
        }
    }

    entryItem(entry) {
        let promptName = entry.promptSelectorKey;
        // the promptSelectorKey is empty if the prompt was entered or changed manually -> use actual prompt.
        if (!promptName || promptName === '-') {
            promptName = entry.prompt.length < 40 ? entry.prompt :
                entry.prompt.substring(0, 30) + ' ... ' + entry.prompt.substring(entry.prompt.length - 10);
        }
        return {
            value: JSON.stringify(entry),
            content: {
                textContent: promptName
            }
        };
    }

    restoreLastPrompts() {
        const items = this.$lastPromptsSelector.get(0).items;
        items.clear();
        items.add({value: '', content: {textContent: ''}});
        this.lastPrompts.forEach((entry) => {
            items.add(this.entryItem(entry));
        });
    }

    onLastPromptsChanged(event) {
        if (this.debug) console.log("onLastPromptsChanged", arguments);
        const value = this.$lastPromptsSelector.val();
        if (value) {
            const entry = JSON.parse(value);
            this.$prompt.val(entry.prompt);
            this.onPromptChanged();
            if (entry.contentSelector) {
                this.$contentSelector.val(entry.contentSelector);
                this.onContentSelectorChanged();
            }
            if (entry.promptSelector) {
                this.$predefinedPromptsSelector.val(entry.promptSelector);
            }
            if (entry.url) {
                this.$urlField.val(entry.url);
                if (entry.contentSelector === 'url') {
                    this.onUrlChanged();
                }
            }
        }
        this.$predefinedPromptsSelector.get(0).scrollIntoView();
    }

    getRte($element) {
        const rte = $element.find('.cq-RichText-editable').data('rteinstance');
        if (!rte) {
            debugger;
        }
        return rte;
    }

    /** Puts the value into the source field. If imagepath is set, we instead make the image visible instead of the source textarea / rte */
    setSourceContent(value, imagepath) {
        console.log("setSourceContent", arguments);
        const $sourceContainer = this.$dialog.find('.composum-ai-source-container');
        $sourceContainer.removeClass('hidden');
        this.$imageContainer.addClass('hidden');
        if (!imagepath) {
            const thevalue = value || '';
            this.isRichtext ? this.$sourceContent.setContent(thevalue) : this.$sourceContent.val(thevalue);
            this.$image.removeData('imagepath');
        } else {
            const $heightReference = $sourceContainer.find('.coral-Form-field');
            const height = $heightReference.height();
            this.$urlContainer.hide();
            $sourceContainer.addClass('hidden');
            this.$imageContainer.removeClass('hidden');
            // const $image = $imageContainer.find('.composum-ai-source-image');
            // this.$image[0].outerHtml = '<div class="coral-Form-field composum-ai-source-image></div>';
            // this.$image = $imageContainer.find('.composum-ai-source-image');
            this.$image.css('background-image', 'url(' + imagepath + ')');
            this.$image.data('imagepath', imagepath);
            this.$image.css('height', height + 'px');
        }
    }

    getSourceContent() {
        return this.isRichtext ? this.$sourceContent.getContent() : this.$sourceContent.val();
    }

    setResponse(value) {
        const thevalue = value || '';
        this.isRichtext ? this.$response.setContent(thevalue) : this.$response.val(thevalue);
    }

    getResponse() {
        return this.isRichtext ? this.$response.getContent() : this.$response.val();
    }

    onSourceContentChanged(event) {
        console.log("onSourceContentChanged", arguments);
        this.$contentSelector.val('-');
    }

    retrieveValue(path, callback) {
        $.ajax({
            url: Granite.HTTP.externalize(APPROXIMATED_MARKDOWN_SERVLET
                + (this.isRichtext ? '.html' : '.md')
                + path
            ),
            type: "GET",
            dataType: "text",
            success: (data, status, xhr) => {
                callback(data, xhr.getResponseHeader('imagepath'));
            },
            error: (xhr, status, error) => {
                console.error("error loading approximate markdown", xhr, status, error);
                this.showError(errorText(status + " " + error));
            }
        });

        // http://localhost:4502/bin/cpm/ai/approximated.md/content/wknd/us/en/magazine/_jcr_content
        // http://localhost:4502/bin/cpm/ai/approximated.html/content/wknd/us/en/magazine/_jcr_content
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
        if (this.debug) console.log("onGenerateButtonClicked", arguments);
        this.showError(undefined);
        let imagepath = this.$image.data('imagepath');
        const data = {
            prompt: this.$prompt.val(),
            source: imagepath ? '' : this.getSourceContent(),
            inputImagePath: imagepath,
            textLength: this.$textLengthSelector.val(),
            richText: this.isRichtext,
            configBasePath: this.pagePath(this.componentPath)
        };
        if (this.debug) console.log("createContent", data);
        this.setLoading(true);
        this.createServlet.createContent(data);
        findSingleElement(this.$dialog, '.composum-ai-actionbar')[0].scrollIntoView();
        // this also makes content suggestion and loading indicator visible.
    }

    streamingCallback(text) {
        // console.log("ContentCreationDialog streamingCallback", arguments);
        this.setResponse(text);
    }

    doneCallback(text, event) {
        this.streamingCallback(text);
        if (this.debug) console.log("ContentCreationDialog doneCallback", arguments);
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
            this.$generateButton.attr('disabled', 'disabled');
            this.$stopButton.removeAttr('disabled');
            findSingleElement(this.$dialog, '.composum-ai-loading').show();
        } else {
            this.$generateButton.removeAttr('disabled');
            this.$stopButton.attr('disabled', 'disabled');
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
            findSingleElement(this.$dialog, '.composum-ai-error-columns')
                .removeClass('hidden').show()[0].scrollIntoView();
            this.$stopButton
            debugger;
        }
    }

    /** Dialog submit: overwrite calling richtext editor / textarea in dialog */
    onSubmit(event) {
        if (this.debug) console.log("ContentCreationDialog onSubmit", arguments);
        try {
            const response = this.getResponse();
            this.closeDialog(event);
            // only after closing since dialog is now out of the way
            if (typeof this.writebackCallback == 'function') {
                this.writebackCallback(response);
            }
        } catch (e) { // better than crashing the whole page
            console.error("Error in onSubmit", e);
        }
    }

    /** Dialog cancel: just closes dialog. */
    onCancel(event) {
        try {
            if (this.debug) console.log("ContentCreationDialog onCancel", arguments);
            this.closeDialog(event);
        } catch (e) { // better than crashing the whole page
            console.error("Error in onCancel", e);
        }
    }

    closeDialog(event) {
        if (this.debug) console.log("ContentCreationDialog closeDialog", arguments);
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
            try {
                this.onFinishCallback();
            } catch (e) {
                console.error("Error in onFinishCallback", e);
            }
        }
    }

}

export {ContentCreationDialog};

// console.log("ContentCreationDialog.js loaded", ContentCreationDialog);

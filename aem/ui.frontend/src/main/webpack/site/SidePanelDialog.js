/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';

const APPROXIMATE_MARKDOWN_SERVLET = '/bin/cpm/ai/approximated.markdown.md';

class SidePanelDialog {
    constructor(dialog) {
        console.log("SidePanelDialog constructor ", arguments);
        this.dialog = $(dialog);
        this.assignElements();
        this.bindActions();
        this.createServlet = new AICreate(this.streamingCallback.bind(this), this.doneCallback.bind(this), this.errorCallback.bind(this));
    }

    findSingleElement(selector) {
        const $el = this.dialog.find(selector);
        if ($el.length !== 1) {
            console.error('BUG! SidebarDialog: missing element for selector', selector, $el, $el.length);
        }
        return $el;
    }

    assignElements() {
        this.$predefinedPromptsSelector = this.findSingleElement('.composum-ai-predefinedprompts');
        this.$contentSelector = this.findSingleElement('.composum-ai-contentselector');
        this.$contentSelector.val('page');
        this.$promptContainer = this.findSingleElement('.composum-ai-promptcontainer');
        this.$promptTemplate = this.findSingleElement('.composum-ai-templates .composum-ai-prompt');
        this.$responseTemplate = this.findSingleElement('.composum-ai-templates .composum-ai-response');
    }

    bindActions() {
        this.$predefinedPromptsSelector.on('change', this.onPredefinedPromptsChanged.bind(this));
        // only for the first prompt container:
        this.findSingleElement('.composum-ai-promptcontainer .composum-ai-prompt').on('change', this.onFirstPromptAreaChanged.bind(this));
        this.findSingleElement('.composum-ai-generate-button').on('click', this.onGenerateButtonClicked.bind(this));
        this.findSingleElement('.composum-ai-stop-button').on('click', function () {
            this.createServlet.abortRunningCalls();
            this.setLoading(false);
        }.bind(this));
        this.findSingleElement('.composum-ai-reset-button').on('click', function () {
            this.ensurePromptCount(1);
            this.$promptContainer.find('.composum-ai-prompt').val('');
            // delete all children of respomnse
            this.$promptContainer.find('.composum-ai-response').children().remove();
        }.bind(this));
        // bind enter key (without any modifiers) in .composum-ai-promptcontainer .composum-ai-prompt to submit
        this.findSingleElement('.composum-ai-promptcontainer').on('keydown', '.composum-ai-prompt', (function (event) {
            if (event.keyCode === 13 && !event.ctrlKey && !event.shiftKey && !event.altKey && !event.metaKey) {
                this.onGenerateButtonClicked(event);
            }
        }).bind(this));
    }

    /** Makes sure there are in composum-ai-promptcontainer exactly n composum-ai-prompt and composum-ai-response (alternating),
     * either by deleting some or by copying some from the templates. */
    ensurePromptCount(n) {
        const currentCount = this.$promptContainer.find('.composum-ai-prompt').size();
        if (currentCount < n) {
            for (let i = currentCount; i < n; i++) {
                this.$promptContainer.append(this.$promptTemplate.clone());
                this.$promptContainer.append(this.$responseTemplate.clone());
            }
        } else {
            for (let i = currentCount; i > n; i--) {
                this.$promptContainer.find('.composum-ai-prompt:last').remove();
                this.$promptContainer.find('.composum-ai-response:last').remove();
            }
        }
    }

    onPredefinedPromptsChanged(event) {
        console.log("onPredefinedPromptsChanged", arguments);
        const prompt = this.$predefinedPromptsSelector.val();
        if (prompt !== '-') {
            this.ensurePromptCount(1);
            this.$promptContainer.find('.composum-ai-prompt').val(prompt);
        }
    }

    onFirstPromptAreaChanged(event) {
        console.log("onPromptAreaChanged", arguments);
        this.$predefinedPromptsSelector.val('-');
    }

    getSelectedPath() {
        const key = this.$contentSelector.val();
        switch (key) {
            case 'component':
                const selectedEditable = Granite.author.selection.getCurrentActive();
                return selectedEditable ? selectedEditable.path : Granite.author.ContentFrame.getContentPath();
            case 'page':
                return Granite.author.ContentFrame.getContentPath();
            case '':
                return undefined;
            default:
                console.error('BUG! ContentCreationDialog: unknown content selector value', key);
        }
        return undefined;
    }

    onGenerateButtonClicked(event) {
        console.log("onGenerateButtonClicked", arguments);
        this.showError(undefined);
        this.$promptContainer.find('.composum-ai-response:last').children().remove();
        // collect chat history from .composum-ai-prompt and .composum-ai-response
        const promptHistory = [];
        this.$promptContainer.find('.composum-ai-prompt').each(function (index, element) {
            promptHistory.push($(element).val());
        });
        const responseHistory = [];
        this.$promptContainer.find('.composum-ai-response').each(function (index, element) {
            responseHistory.push(element.textContent);
        });
        // join promptHistory and responseHistory into a single array, format:
        // [{"role":"USER","content":"Hi!"}, {"role":"ASSISTANT","content":"Hi! How can I help you?"}, ...].
        const history = [];
        for (let i = 0; i < promptHistory.length; i++) {
            if (i > 0) { // the first prompt is the initial prompt transmitted as prompt parameter
                history.push({"role": "USER", "content": promptHistory[i]});
            }
            history.push({"role": "ASSISTANT", "content": responseHistory[i]});
        }

        const data = {
            prompt: this.$promptContainer.find('.composum-ai-prompt:first').val(),
            chat: JSON.stringify(history),
            sourcePath: this.getSelectedPath()
        };
        console.log("createContent", data);
        this.setLoading(true);
        this.createServlet.createContent(data);
    }

    streamingCallback(data) {
        console.log("ContentCreationDialog streamingCallback", arguments);
        // set the text of the last div.composum-ai-response to the data
        const lastResponse = this.$promptContainer.find('.composum-ai-response:last');
        lastResponse.text(data);
    }

    doneCallback(data) {
        this.ensurePromptCount(this.$promptContainer.find('.composum-ai-response').size() + 1);
        console.log("ContentCreationDialog doneCallback", arguments);
        if (data && data.data && data.data.result && data.data.result.finishreason === 'STOP') {
            this.showError('The generated content stopped because of the length restriction.');
        } else {
            this.showError(undefined);
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
            this.findSingleElement('.composum-ai-loading').removeClass('hidden');
        } else {
            this.findSingleElement('.composum-ai-generate-button').removeAttr('disabled');
            this.findSingleElement('.composum-ai-loading').addClass('hidden');
        }
    }

    /** Shows the error text if error is given, hides it if it's falsy. */
    showError(error) {
        if (!error) {
            this.findSingleElement('.composum-ai-error-columns').addClass('hidden');
        } else {
            console.error("ContentCreationDialog showError", arguments);
            this.findSingleElement('.composum-ai-alert').text(error);
            this.findSingleElement('.composum-ai-error-columns').removeClass('hidden');
        }
    }

}

export {SidePanelDialog};

console.log("SidePanelDialog.js loaded", SidePanelDialog);

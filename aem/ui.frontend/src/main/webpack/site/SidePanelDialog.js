/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';

const APPROXIMATE_MARKDOWN_SERVLET = '/bin/cpm/ai/approximated.markdown.md';

class SidePanelDialog {
    constructor(dialog) {
        console.log("SidePanelDialog constructor ", arguments, this);
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
        this.$promptContainer.on('change', '.composum-ai-prompt', this.onPromptAreaChanged.bind(this));
        this.$promptContainer.on('focus', '.composum-ai-prompt', this.expandOnFocus);
        this.$promptContainer.on('blur', '.composum-ai-prompt', this.shrinkOnBlur);
        this.findSingleElement('.composum-ai-generate-button').on('click', this.onGenerateButtonClicked.bind(this));
        this.findSingleElement('.composum-ai-stop-button').on('click', function () {
            this.createServlet.abortRunningCalls();
            this.setLoading(false);
        }.bind(this));
        this.findSingleElement('.composum-ai-reset-button').on('click', function () {
            this.ensurePromptCount(1);
            this.$promptContainer.find('.composum-ai-prompt').val('');
            this.$promptContainer.find('.composum-ai-response').text('');
        }.bind(this));
        // bind enter key (without any modifiers) in .composum-ai-promptcontainer .composum-ai-prompt to submit
        this.findSingleElement('.composum-ai-promptcontainer').on('keydown', '.composum-ai-prompt', (function (event) {
            if (event.keyCode === 13 && !event.ctrlKey && !event.shiftKey && !event.altKey && !event.metaKey) {
                this.onGenerateButtonClicked(event);
            }
        }).bind(this));
    }

    /** Makes sure there are in composum-ai-promptcontainer exactly n composum-ai-prompt and composum-ai-response (alternating),
     * either by deleting some or by copying some from the templates. If n < 0 we remove that many, but keep at least one. */
    ensurePromptCount(n) {
        const currentCount = this.$promptContainer.find('.composum-ai-prompt').length;
        n = n < 0 ? Math.max(currentCount + n, 1) : n === 0 ? 1 : n;
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

    removeLastEmptyPrompts() {
        while (!this.$promptContainer.find('.composum-ai-prompt:last').val() && this.$promptContainer.find('.composum-ai-prompt').length > 1) {
            this.ensurePromptCount(-1);
        }
    }

    /** If the user presses enter in one of the prompt textareas, we remove all after that one by calling ensurePromptCount with the number of that textarea. */
    removePromptsAfterEventSource(event) {
        const $target = $(event.target);
        if ($target.hasClass('composum-ai-prompt')) {
            const index = this.$promptContainer.find('.composum-ai-prompt').index($target);
            if (index >= 0) {
                this.ensurePromptCount(index + 1);
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

    onPromptAreaChanged(event) {
        console.log("onPromptAreaChanged", arguments);
        this.$predefinedPromptsSelector.val('-');
    }

    // TODO: possibly use resize on typing https://stackoverflow.com/questions/454202/creating-a-textarea-with-auto-resize/77155208
    expandOnFocus(event) {
        var that = event.target;
        that.rows = 5;
        while (that.scrollHeight > that.clientHeight) {
            that.rows++;
        }
        that.rows += 5;
        that.scrollTop = that.scrollHeight;
    }

    /** Shrink the text area to it's actual size so that it just captures the text it has. */
    shrinkOnBlur(event) {
        var that = event.target;
        $(that).val($(that).val().trim());
        setTimeout(function () {
            that.rows = 1;
            while (that.scrollHeight > that.clientHeight) {
                that.rows++;
            }
        }, 100);
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
        this.removeLastEmptyPrompts();
        this.removePromptsAfterEventSource(event);
        this.$promptContainer.find('.composum-ai-response:last').text('');
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
        history.pop(); // remove empty assistant message at the end

        const data = {
            prompt: this.$promptContainer.find('.composum-ai-prompt:first').val(),
            chat: JSON.stringify(history),
            sourcePath: this.getSelectedPath()
        };
        console.log("createContent", data);
        this.setLoading(true);
        this.createServlet.createContent(data);
    }

    streamingCallback(text) {
        // console.log("ContentCreationDialog streamingCallback", arguments);
        // set the text of the last div.composum-ai-response to the data
        const lastResponse = this.$promptContainer.find('.composum-ai-response:last');
        lastResponse.text(text);
    }

    doneCallback(text, event) {
        this.ensurePromptCount(this.$promptContainer.find('.composum-ai-response').length + 1);
        this.$promptContainer.find('.composum-ai-prompt:last').focus();
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
            debugger;
            const errorText = typeof error === 'string' ? error : JSON.stringify(error);
            this.findSingleElement('.composum-ai-alert coral-alert-content').text(errorText);
            this.findSingleElement('.composum-ai-error-columns').removeClass('hidden');
        }
    }

}

export {SidePanelDialog};

console.log("SidePanelDialog.js loaded", SidePanelDialog);

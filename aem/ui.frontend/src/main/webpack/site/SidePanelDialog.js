/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';
import {contentFragmentPath, errorText, findSingleElement, coralSelectValue} from './common.js';
import {DialogHistory} from './DialogHistory.js';
import {HelpPage} from './HelpPage.js';

/** Keeps dialog histories per path. */
const historyMap = {};

class SidePanelDialog {

    debug = true;
    verbose = false;

    constructor(dialog) {
        console.log("SidePanelDialog constructor ", arguments, this);
        this.$dialog = $(dialog);
        this.assignElements();
        this.bindActions();
        this.showError(null);
        this.setLoading(false);
        findSingleElement(this.$dialog, '.composum-ai-templates').hide(); // class hidden isn't present in content fragment editor
        this.createServlet = new AICreate(this.streamingCallback.bind(this), this.doneCallback.bind(this), this.errorCallback.bind(this));

        const historyPath = this.getContentPath();
        if (!historyMap[historyPath]) {
            historyMap[historyPath] = [];
        }
        if (this.debug) console.log("SidePanelDialog historyPath", historyPath);
        this.history = new DialogHistory(this.$dialog, this.getDialogStatus.bind(this), this.setDialogStatus.bind(this), historyMap[historyPath]);
        setTimeout(() => this.history.restoreFromLastOfHistory(), 50); // coral-selects are not ready yet
    }

    assignElements() {
        this.$predefinedPromptsSelector = findSingleElement(this.$dialog, '.composum-ai-predefinedprompts');
        this.$contentSelector = findSingleElement(this.$dialog, '.composum-ai-contentselector');
        this.$promptContainer = findSingleElement(this.$dialog, '.composum-ai-promptcontainer');
        this.$promptTemplate = findSingleElement(this.$dialog, '.composum-ai-templates .composum-ai-prompt');
        this.$responseTemplate = findSingleElement(this.$dialog, '.composum-ai-templates .composum-ai-response');
        this.$stopButton = findSingleElement(this.$dialog, '.composum-ai-stop-button');
        this.$generateButton = findSingleElement(this.$dialog, '.composum-ai-generate-button');
    }

    bindActions() {
        this.$predefinedPromptsSelector.on('change', this.onPredefinedPromptsChanged.bind(this));
        // only for the first prompt container:
        this.$promptContainer.on('change input', '.composum-ai-prompt', this.onPromptAreaChanged.bind(this));
        this.$promptContainer.on('focus', '.composum-ai-prompt', this.expandOnFocus);
        this.$promptContainer.on('blur', '.composum-ai-prompt', this.shrinkOnBlur);
        this.$generateButton.on('click', this.onGenerateButtonClicked.bind(this));
        this.$stopButton.on('click', this.onStopClicked.bind(this));
        findSingleElement(this.$dialog, '.composum-ai-reset-button').on('click', this.resetForm.bind(this));
        // bind enter key (without any modifiers) in .composum-ai-promptcontainer .composum-ai-prompt to submit
        findSingleElement(this.$dialog, '.composum-ai-promptcontainer').on('keydown', '.composum-ai-prompt', (function (event) {
            if (event.keyCode === 13 && !event.ctrlKey && !event.shiftKey && !event.altKey && !event.metaKey) {
                event.preventDefault();
                this.onGenerateButtonClicked(event);
            }
        }).bind(this));
        this.setLoading(false);
        findSingleElement(this.$dialog, '.composum-ai-help-button').on('click', (event) => new HelpPage(event).show());
    }

    onStopClicked(event) {
        if (this.debug) console.log("onStopClicked", arguments);
        if (event) {
            event.preventDefault();
        }
        this.createServlet.abortRunningCalls();
        this.setLoading(false);
        this.history.maybeSaveToHistory();
    }

    resetForm(event) {
        this.onStopClicked(event);
        this.setDialogStatus({});
    }

    getDialogStatus() {
        return {
            predefinedPrompts: coralSelectValue(this.$predefinedPromptsSelector),
            contentSelector: coralSelectValue(this.$contentSelector),
            promptCount: this.$promptContainer.find('.composum-ai-prompt').length,
            prompts: this.$promptContainer.find('.composum-ai-prompt').map(function () {
                return $(this).val();
            }).get(),
            responses: this.$promptContainer.find('.composum-ai-response').map(function () {
                return $(this).text();
            }).get()
        };
    }

    setDialogStatus(status) {
        if (this.debug) console.log("SidePanelDialog setDialogStatus", status);
        coralSelectValue(this.$predefinedPromptsSelector, status.predefinedPrompts);
        coralSelectValue(this.$contentSelector, status.contentSelector || 'page');
        if (status.promptCount) {
            this.ensurePromptCount(status.promptCount);
            this.$promptContainer.find('.composum-ai-prompt').each(function (index, element) {
                $(element).val(status.prompts[index]);
            });
            this.$promptContainer.find('.composum-ai-response').each(function (index, element) {
                $(element).text(status.responses[index]);
            });
        } else {
            this.ensurePromptCount(1);
            this.$promptContainer.find('.composum-ai-prompt').val('');
            this.$promptContainer.find('.composum-ai-response').text('');
        }
        this.setAutomaticGenerateButtonState();
    }

    /** Makes sure there are in composum-ai-promptcontainer exactly n composum-ai-prompt and composum-ai-response (alternating),
     * either by deleting some or by copying some from the templates. If n < 0 we remove that many, but keep at least one. */
    ensurePromptCount(n) {
        const currentCount = this.$promptContainer.find('.composum-ai-prompt').length;
        if (this.debug) console.log("ensurePromptCount", n, currentCount);
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
        if (this.debug) console.log("onPredefinedPromptsChanged", arguments);
        const prompt = coralSelectValue(this.$predefinedPromptsSelector);
        if (prompt !== '-') {
            this.ensurePromptCount(1);
            const promptArea = this.$promptContainer.find('.composum-ai-prompt');
            promptArea.val(prompt);
            this.expandOnFocus({target: promptArea[0]});
            this.setAutomaticGenerateButtonState();
        }
    }

    onPromptAreaChanged(event) {
        if (this.verbose) console.log("onPromptAreaChanged", arguments); // on each key press
        coralSelectValue(this.$predefinedPromptsSelector, '-');
        this.setAutomaticGenerateButtonState();
    }

    // TODO: possibly use resize on typing https://stackoverflow.com/questions/454202/creating-a-textarea-with-auto-resize/77155208
    expandOnFocus(event) {
        if (this.debug) console.log("expandOnFocus", arguments);
        var that = event.target;
        if (!$(that).is('textarea')) {
            console.error("BUG! expandOnFocus called on non-textarea", that);
            return;
        }
        that.rows = 5;
        while (that.rows <= 20 && that.scrollHeight > that.clientHeight) {
            that.rows++;
        }
        that.rows += 5;
        that.scrollTop = that.scrollHeight;
    }

    /** Shrink the text area to it's actual size so that it just captures the text it has. */
    shrinkOnBlur(event) {
        if (this.debug) console.log("shrinkOnBlur", event);
        var that = event.target;
        if (!$(that).is('textarea')) {
            console.error("BUG! shrinkOnBlur called on non-textarea", that);
            return;
        }
        $(that).val($(that).val().trim());
        setTimeout(function () {
            that.rows = 1;
            while (that.rows <= 20 && that.scrollHeight > that.clientHeight) {
                that.rows++;
            }
        }, 100);
    }

    getSelectedPath() {
        const key = coralSelectValue(this.$contentSelector);
        var contentPath = this.getContentPath();
        var path;
        switch (key) {
            case 'component':
                const selectedEditable = Granite.author.selection.getCurrentActive();
                path = selectedEditable ? selectedEditable.path : contentPath;
                break;
            case 'page':
                path = contentPath;
                break;
            case '':
                path = undefined;
                break;
            default:
                console.error('BUG! SidePanelDialog: unknown content selector value', key);
                break;
        }
        console.log("SidePanelDialog getSelectedPath", key, path);
        return path;
    }

    getContentPath() {
        var contentPath = Granite.author.ContentFrame.getContentPath();
        if (!contentPath || !contentPath.startsWith('/')) {
            contentPath = contentFragmentPath();
        }
        return contentPath;
    }

    onGenerateButtonClicked(event) {
        if (this.debug) console.log("onGenerateButtonClicked", arguments);
        event.preventDefault();
        this.showError(undefined);
        this.removeLastEmptyPrompts();
        this.shrinkOnBlur({target: this.$promptContainer.find('.composum-ai-prompt:last')[0]});
        this.removePromptsAfterEventSource(event);
        this.$promptContainer.find('.composum-ai-response:last').text('');
        this.$promptContainer.find('.composum-ai-response:last')[0].scrollIntoView({
            behavior: "smooth",
            block: "center"
        });
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
        // [{"role":"user","content":"Hi!"}, {"role":"assistant","content":"Hi! How can I help you?"}, ...].
        const chatHistory = [];
        for (let i = 0; i < promptHistory.length; i++) {
            if (i > 0) { // the first prompt is the initial prompt transmitted as prompt parameter
                chatHistory.push({"role": "user", "content": promptHistory[i]});
            }
            chatHistory.push({"role": "assistant", "content": responseHistory[i]});
        }
        chatHistory.pop(); // remove empty assistant message at the end

        const data = {
            prompt: this.$promptContainer.find('.composum-ai-prompt:first').val(),
            chat: JSON.stringify(chatHistory),
            configBasePath: this.getContentPath()
        };
        const sourcePath = this.getSelectedPath();
        if (sourcePath) {
            data.sourcePath = sourcePath;
        }
        if (this.debug) console.log("createContent", data);
        this.setLoading(true);
        this.createServlet.createContent(data);
    }

    streamingCallback(text) {
        if (this.debug) console.log("SidePanelDialog streamingCallback", arguments);
        // set the text of the last div.composum-ai-response to the data
        const lastResponse = this.$promptContainer.find('.composum-ai-response:last');
        lastResponse.text(text);
    }

    doneCallback(text, event) {
        if (this.debug) console.log("SidePanelDialog doneCallback", arguments);
        this.ensurePromptCount(this.$promptContainer.find('.composum-ai-response').length + 1);
        this.$promptContainer.find('.composum-ai-prompt:last').focus();
        console.log("SidePanelDialog doneCallback", arguments);
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
        console.error("SidePanelDialog errorCallback", arguments);
        this.showError(data);
        this.setLoading(false);
    }

    setLoading(loading) {
        if (loading) {
            this.$generateButton.attr('disabled', 'disabled');
            this.$stopButton.removeAttr('disabled');
            findSingleElement(this.$dialog, '.composum-ai-loading').show();
        } else {
            this.setAutomaticGenerateButtonState();
            this.$stopButton.attr('disabled', 'disabled');
            findSingleElement(this.$dialog, '.composum-ai-loading').hide();
        }
    }

    setAutomaticGenerateButtonState() {
        if (this.$promptContainer.find('.composum-ai-prompt').val()) {
            this.$generateButton.removeAttr('disabled');
        } else {
            this.$generateButton.attr('disabled', 'disabled');
        }
    }

    /** Shows the error text if error is given, hides it if it's falsy. */
    showError(error) {
        if (!error) {
            findSingleElement(this.$dialog, '.composum-ai-error-columns').hide();
        } else {
            console.error("SidePanelDialog showError", arguments);
            findSingleElement(this.$dialog, '.composum-ai-alert coral-alert-content')
                .text(errorText(error));
            findSingleElement(this.$dialog, '.composum-ai-error-columns')
                .removeClass('hidden').show()[0].scrollIntoView();
            debugger;
        }
    }

}

export {SidePanelDialog};

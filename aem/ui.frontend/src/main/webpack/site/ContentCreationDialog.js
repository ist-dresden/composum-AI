/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

import {AICreate} from './AICreate.js';

const APPROXIMATE_MARKDOWN_SERVLET = '/bin/cpm/ai/approximated.markdown.md';

class ContentCreationDialog {
    constructor(editable, dialog, oldContent) {
        console.log("ContentCreationDialog constructor ", arguments);
        this.editable = editable;
        this.dialog = $(dialog);
        this.oldContent = oldContent;
        this.assignElements();
        this.bindActions();
        this.setSourceContentArea(oldContent);
        this.createServlet = new AICreate(this.streamingCallback.bind(this), this.doneCallback.bind(this), this.errorCallback.bind(this));
    }

    findSingleElement(selector) {
        const $el = this.dialog.find(selector);
        if ($el.length !== 1) {
            console.error('BUG! SidebarDialog: missing element for selector', selector, $el);
        }
        return $el;
    }

    assignElements() {
        this.$promptArea = this.findSingleElement('.composum-ai-prompt-textarea');
        this.$predefinedPromptsSelector = this.findSingleElement('.composum-ai-predefined-prompts');
        this.$contentSelector = this.findSingleElement('.composum-ai-content-selector');
        this.$sourceContentArea = this.findSingleElement('.composum-ai-source-content');
        this.$textLengthSelector = this.findSingleElement('.composum-ai-text-length-selector');
        this.$responseArea = this.findSingleElement('.composum-ai-response-field');
    }

    bindActions() {
        this.$predefinedPromptsSelector.on('change', this.onPredefinedPromptsChanged.bind(this));
        this.$promptArea.on('change', this.onPromptAreaChanged.bind(this));
        this.$contentSelector.on('change', this.onContentSelectorChanged.bind(this));
        this.$sourceContentArea.on('change', this.onSourceContentAreaChanged.bind(this));
        this.findSingleElement('.composum-ai-generate-button').on('click', this.onGenerateButtonClicked.bind(this));
    }

    onPredefinedPromptsChanged(event) {
        console.log("onPredefinedPromptsChanged", arguments);
        const prompt = this.$predefinedPromptsSelector.val();
        if (prompt !== '-') {
            this.$promptArea.val(prompt);
        }
    }

    onPromptAreaChanged(event) {
        console.log("onPromptAreaChanged", arguments);
        this.$predefinedPromptsSelector.val('-');
    }

    onContentSelectorChanged(event) {
        console.log("onContentSelectorChanged", arguments);
        // possible values widget, component, page, lastoutput, -
        const key = this.$contentSelector.val();
        switch (key) {
            case 'lastoutput':
                this.setSourceContentArea(this.$responseArea.val());
                break;
            case 'widget':
                this.setSourceContentArea(this.oldContent);
                break;
            case 'component':
                this.retrieveValue(this.editable.path, (value) => this.setSourceContentArea(value));
                break;
            case 'page':
                this.retrieveValue(this.pagePath(this.editable.path), (value) => this.setSourceContentArea(value));
                break;
            case '-':
                break;
            default:
                console.error('BUG! ContentCreationDialog: unknown content selector value', key);
        }
    }

    setSourceContentArea(value) {
        this.$sourceContentArea.val(value);
    }

    onSourceContentAreaChanged(event) {
        console.log("onSourceContentAreaChanged", arguments);
        this.$contentSelector.val('-');
    }

    retrieveValue(path, callback) {
        $.ajax({
            url: Granite.HTTP.externalize(APPROXIMATE_MARKDOWN_SERVLET + path),
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
        return path.substring(0, path.lastIndexOf('/jcr:content') + '/jcr:content'.length);
    }

    onGenerateButtonClicked(event) {
        console.log("onGenerateButtonClicked", arguments);
        const data = {
            prompt: this.$promptArea.val(),
            source: this.$sourceContentArea.val(),
            textLength: this.$textLengthSelector.val()
        };
        console.log("createContent", data);
        this.createServlet.createContent(data);
    }

    streamingCallback(data) {
        console.log("ContentCreationDialog streamingCallback", arguments);
        this.$responseArea.val(data); // XXX
    }

    doneCallback(data) {
        console.log("ContentCreationDialog doneCallback", arguments);
        this.$responseArea.val(data); // XXX
    }

    errorCallback(data) {
        console.log("ContentCreationDialog errorCallback", arguments);
        this.$responseArea.val(data); // XXX
    }

}

export {ContentCreationDialog};

console.log("ContentCreationDialog.js loaded", ContentCreationDialog);

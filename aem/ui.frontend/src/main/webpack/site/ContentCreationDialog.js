/** Implementation for the actions of the Content Creation Dialog - button actions, drop down list actions etc. */

class ContentCreationDialog {
    constructor(editable, dialog) {
        console.log("ContentCreationDialog constructor ", arguments);
        this.editable = editable;
        this.dialog = $(dialog);
        this.assignElements();
        this.bindActions();
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
        // TODO implement
    }

}

export {ContentCreationDialog};

console.log("ContentCreationDialog.js loaded", ContentCreationDialog);

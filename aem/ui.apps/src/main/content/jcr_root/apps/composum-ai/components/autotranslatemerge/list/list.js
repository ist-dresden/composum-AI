/* AIGenVersion(5f5169a7, list.js.prompt-6609a4cf, 8.2AutomaticTranslationMergeTool.md-4eae22e4, list.html-62d682fb) */

/** Handles the general script functionality for the Translation Merge Tool. */
class AITranslateMergeTool {
    constructor() {
        document.addEventListener("DOMContentLoaded", () => {
            this.initRTEEditors();
            this.initTableEventListeners();
        });
    }

    /** Initializes Rich Text Editors by instantiating AITranslatorMergeRTE for each editor container. */
    initRTEEditors() {
        const rteContainers = document.querySelectorAll(".rte-container");
        rteContainers.forEach(container => {
            new AITranslatorMergeRTE(container);
        });
    }

    /** Initializes table event listeners for handling row-specific actions. */
    initTableEventListeners() {
        const tableBody = document.querySelector(".propertiestable");
        document.querySelectorAll("tbody tr").forEach(row => {
            new AITranslateMergeRow(row, this);
        });
    }

    /** Performs an intelligent merge of current and new text. */
    intelligentMerge(currentText, newText) {
        // Implement AI-based merge logic here
        // For now, it simply appends the new text
        return currentText + newText;
    }
}

/** Handles copy, append, save, and intelligent merge actions for each table row. */
class AITranslateMergeRow {
    constructor(row, tool) {
        this.row = row;
        this.tool = tool;
        this.editorContainer = row.querySelector(".rte-container");
        if (this.editorContainer) {
            this.editor = this.editorContainer.querySelector(".rte-editor");
            this.saveButton = this.row.querySelector(".save-editor");

            this.copyButton = this.row.querySelector(".copy-to-editor");
            this.appendButton = this.row.querySelector(".append-to-editor");
            this.mergeButton = this.row.querySelector(".intelligent-merge");

            this.resetButton = this.row.querySelector(".reset-editor");

            new AITranslatorMergeRTE(this.editorContainer, this.editorContainer.querySelector(".save-editor"));

            this.copyButton.addEventListener("click", this.copyToEditor.bind(this));
            this.appendButton.addEventListener("click", this.appendToEditor.bind(this));
            this.mergeButton.addEventListener("click", this.intelligentMerge.bind(this));

            this.resetButton.addEventListener("click", this.resetEditor.bind(this));
        }
    }

    copyToEditor() {
        this.editor.innerHTML = this.row.dataset.nt;
        this.saveButton.disabled = false;
    }

    appendToEditor() {
        this.editor.innerHTML += this.row.dataset.nt;
        this.saveButton.disabled = false;
    }

    resetEditor() {
        this.editor.innerHTML = this.row.dataset.e;
        this.saveButton.disabled = true;
    }

    intelligentMerge() {
        console.log("TODO IMPLEMENT Intelligent merge");
        this.saveButton.disabled = false;
    }
}

/** Manages the rich text editor functionalities, including toolbar actions and save/reset operations. */
class AITranslatorMergeRTE {
    constructor(container, saveButton) {
        this.editor = container.querySelector(".rte-editor");
        this.toolbar = container.querySelector(".rte-toolbar");
        this.saveButton = saveButton;

        this.toolbar.addEventListener("click", this.handleToolbarClick.bind(this));
        this.editor.addEventListener("input", this.handleEditorInput.bind(this));
    }

    handleToolbarClick(event) {
        const command = event.target.dataset.command;
        if (command) {
            document.execCommand(command, false, null);
            this.editor.focus(); // Refocus the editor after the action
        }
    }

    handleEditorInput() {
        this.saveButton.disabled = false;
    }
}

new AITranslateMergeTool();

console.log("AutoTranslateMerge list.js loaded");

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
        tableBody.addEventListener("click", (event) => {
            const target = event.target;
            if (target.matches(".copy-to-editor, .append-to-editor, .intelligent-merge")) {
                const row = target.closest("coral-table-row");
                new AITranslateMergeRow(row, target, this);
            }
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
    constructor(row, target, tool) {
        this.row = row;
        this.target = target;
        this.tool = tool;
        this.handleAction();
    }

    /** Determines and executes the action based on the button clicked. */
    handleAction() {
        const newTransCell = this.row.querySelector(".newtrans");
        const editorContainer = this.row.querySelector(".rte-container");
        const editor = editorContainer.querySelector(".rte-editor");
        const newText = newTransCell.textContent.trim();
        const saveButton = this.row.querySelector(".save-editor");

        if (this.target.classList.contains("copy-to-editor")) {
            this.copyToEditor(editor, newText, saveButton);
        } else if (this.target.classList.contains("append-to-editor")) {
            this.appendToEditor(editor, newText, saveButton);
        } else if (this.target.classList.contains("intelligent-merge")) {
            this.intelligentMerge(editor, newText, saveButton);
        }
    }

    /** Copies new text to the editor, overwriting existing content. */
    copyToEditor(editor, newText, saveButton) {
        editor.innerHTML = newText;
        editor.setAttribute('data-original-content', newText);
        saveButton.disabled = true;
    }

    /** Appends new text to the existing content in the editor. */
    appendToEditor(editor, newText, saveButton) {
        editor.innerHTML += newText;
        saveButton.disabled = false;
    }

    /** Merges new text with existing content using intelligent merge logic. */
    intelligentMerge(editor, newText, saveButton) {
        const mergedText = this.tool.intelligentMerge(editor.innerHTML, newText);
        editor.innerHTML = mergedText;
        editor.setAttribute('data-original-content', mergedText);
        saveButton.disabled = false;
    }
}

/** Manages the rich text editor functionalities, including toolbar actions and save/reset operations. */
class AITranslatorMergeRTE {

    constructor(container) {
        this.editor = container.querySelector(".rte-editor");
        this.toolbar = container.querySelector(".rte-toolbar");
        this.resetButton = container.querySelector(".reset-editor");
        this.saveButton = container.querySelector(".save-editor");

        this.toolbar.addEventListener("click", (event) => {
            const command = event.target.dataset.command;
            if (command) {
                document.execCommand(command, false, null);
                this.editor.focus(); // Refocus the editor after the action
            }
        });

        this.resetButton.addEventListener("click", () => {
            const originalContent = this.editor.getAttribute('data-original-content');
            if (originalContent !== null) {
                this.editor.innerHTML = originalContent;
                this.saveButton.disabled = true;
            }
        });

        this.editor.addEventListener("input", () => {
            const currentContent = this.editor.innerHTML;
            const originalContent = this.editor.getAttribute('data-original-content');
            this.saveButton.disabled = (currentContent === originalContent);
        });

        this.saveButton.addEventListener("click", () => {
            const updatedContent = this.editor.innerHTML;
            // Implement save functionality here
            this.editor.setAttribute('data-original-content', updatedContent);
            this.saveButton.disabled = true;
        });
    }
}

new AITranslateMergeTool();

console.log("AutoTranslateMerge list.js loaded");

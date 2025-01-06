const URL_MERGE_SERVLET='/bin/cpm/ai/aitranslationmerge';

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
            this.saveButton = this.row.querySelector(".save-editor");

            new AITranslatorMergeRTE(this.editorContainer, this.saveButton);

            this.copyButton.addEventListener("click", this.copyToEditor.bind(this));
            this.appendButton.addEventListener("click", this.appendToEditor.bind(this));
            this.mergeButton.addEventListener("click", this.intelligentMerge.bind(this));

            this.resetButton.addEventListener("click", this.resetEditor.bind(this));
            this.saveButton.addEventListener("click", this.saveEditor.bind(this));
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
        const data = {
            path: this.row.dataset.path,
            propertyName: this.row.dataset.propertyname,
            originalSource: this.row.dataset.os,
            newSource: this.row.dataset.ns,
            newTranslation: this.row.dataset.nt,
            currentText: this.editor.innerHTML
        };

        Granite.csrf.refreshToken().then(token => {
            fetch(URL_MERGE_SERVLET + "?operation=merge", {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'CSRF-Token': token
                },
                body: JSON.stringify({
                    operation: 'merge',
                    ...data
                })
            })
            .then(response => {
                if (response.ok) {
                    return response.text();
                } else {
                    return response.text().then(errMsg => {
                        throw new Error("Merge failed: " + errMsg);
                    });
                }
            })
            .then(mergedText => {
                this.editor.innerHTML = mergedText;
                this.saveButton.disabled = false;
                console.log("Merge successful");
            })
            .catch(error => {
                console.error("Error in intelligentMerge", error);
            });
        });
    }

    saveEditor() {
        Granite.csrf.refreshToken().then(token => {
            fetch(URL_MERGE_SERVLET + "?operation=save", {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'CSRF-Token': token
                },
                body: new URLSearchParams({
                    path: this.row.dataset.path,
                    propertyName: this.row.dataset.propertyname,
                    body: this.editor.innerHTML
                })
            })
                .then(response => {
                    if (response.ok) {
                        console.log("Save successful");
                        this.saveButton.disabled = true;
                    } else {
                        return response.text().then(errMsg => {
                            throw new Error("Save failed: " + errMsg);
                        });
                    }
                })
                .catch(error => {
                    console.error("Error in saveEditor", error);
                });
        });
    }
}

/** Manages the rich text editor functionalities, including toolbar actions and save/reset operations. */
class AITranslatorMergeRTE {
    constructor(container, saveButton) {
        this.editor = container.querySelector(".rte-editor");
        this.toolbar = container.querySelector(".rte-toolbar");
        this.saveButton = saveButton;

        this.toolbar.addEventListener("click", this.handleToolbarClick.bind(this));
        this.editor.addEventListener("keyup", this.handleEditorInput.bind(this));
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

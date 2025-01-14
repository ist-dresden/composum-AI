const URL_MERGE_SERVLET = '/bin/cpm/ai/aitranslationmerge';

/** Handles the general script functionality for the Translation Merge Tool. */
class AITranslateMergeTool {
    constructor() {
        document.addEventListener("DOMContentLoaded", () => {
            this.initTableEventListeners();
            this.initNavButtons();
            this.initFooterButtons();
            document.querySelectorAll('coral-tooltip').forEach(tooltip => tooltip.delay = 1000);
        });
    }

    /** Initializes table event listeners for handling row-specific actions. */
    initTableEventListeners() {
        const tableBody = document.querySelector(".propertiestable");
        document.querySelectorAll("tbody tr.datarow").forEach(row => {
            const id = row.dataset.id;
            const actionrow = document.getElementById("actionrow-" + id);
            new AITranslateMergeRow(row, actionrow, this);
        });
    }

    /** For anchors with data-forwardid or data-backwardid set the href to #(id+1) / #(id-1). */
    initNavButtons() {
        document.querySelectorAll('[data-forwardid]').forEach(button => {
            const id = parseInt(button.dataset.forwardid);
            button.href = `#row-${id + 1}`;
        });
        document.querySelectorAll('[data-backwardid]').forEach(button => {
            const id = parseInt(button.dataset.backwardid);
            button.href = `#row-${id - 1}`;
        });
    }

    initFooterButtons() {
        document.querySelector('.toggle-diffs').addEventListener('click', this.toggleDiffs.bind(this));
        document.querySelector('.toggle-current').addEventListener('click', this.toggleCurrent.bind(this));
    }

    toggleDiffs() {
        document.body.classList.toggle('show-diffs');
        document.body.classList.toggle('hide-diffs');
    }

    toggleCurrent() {
        document.body.classList.toggle('show-currenttext');
        document.body.classList.toggle('hide-currenttext');
    }

    /** If message is null the error field is hidden. */
    showError(message) {
        const errormessage = document.querySelector('.errormessage');
        const alertcontent = errormessage.querySelector('.alertcontent');
        if (message) {
            alertcontent.textContent = message;
            errormessage.hidden = false;
        } else {
            errormessage.hidden = true;
        }
    }

}

/** Handles copy, append, save, and intelligent merge actions for each table row. */
class AITranslateMergeRow {
    constructor(row, actionrow, tool) {
        this.row = row;
        this.actionrow = actionrow;
        this.tool = tool;
        this.editorContainer = row.querySelector(".rte-container");
        if (this.editorContainer) {
            this.editor = this.editorContainer.querySelector(".rte-editor");
        } else {
            this.editorContainer = row.querySelector(".text-container");
            this.editor = this.editorContainer?.querySelector(".text-editor");
        }

        this.copyButton = this.actionrow.querySelector(".copy-to-editor");
        this.appendButton = this.actionrow.querySelector(".append-to-editor");
        this.mergeButton = this.actionrow.querySelector(".intelligent-merge");

        this.resetButton = this.actionrow.querySelector(".reset-editor");
        this.saveButton = this.actionrow.querySelector(".save-editor");

        new AITranslatorMergeRTE(this.editorContainer, this.saveButton);

        this.copyButton.addEventListener("click", this.copyToEditor.bind(this));
        if (this.appendButton) this.appendButton.addEventListener("click", this.appendToEditor.bind(this));
        this.mergeButton.addEventListener("click", this.intelligentMerge.bind(this));

        this.resetButton.addEventListener("click", this.resetEditor.bind(this));
        this.saveButton.addEventListener("click", this.saveEditor.bind(this));
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
            currentText: this.editor.innerHTML,
            language: this.row.dataset.language
        };
        const btn = this.mergeButton;

        Granite.csrf.refreshToken().then(token => {
            btn.disabled = true;
            btn.classList.add('activespinner');
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
                    this.tool.showError(null);
                })
                .catch(error => {
                    console.error("Error in intelligentMerge", error);
                    this.tool.showError(errMsg);
                }).finally(() => {
                btn.disabled = false;
                btn.classList.remove('activespinner');
            });
        });
    }

    saveEditor() {
        const btn = this.saveButton;
        const row = this.row;
        Granite.csrf.refreshToken().then(token => {
            btn.disabled = true;
            btn.classList.add('activespinner');
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
                        row.classList.add("merged");
                    } else {
                        return response.text().then(errMsg => {
                            throw new Error("Save failed: " + errMsg);
                        });
                    }
                })
                .catch(error => {
                    console.error("Error in saveEditor", error);
                    this.tool.showError(errMsg);
                }).finally(() => {
                btn.disabled = false;
                btn.classList.remove('activespinner');
            });
        });
    }
}

/** Manages the rich text editor functionalities, including toolbar actions and save/reset operations. */
class AITranslatorMergeRTE {
    constructor(container, saveButton) {
        this.editor = container.querySelector(".rte-editor") || container.querySelector(".text-editor");
        this.toolbar = container.querySelector(".rte-toolbar");
        this.saveButton = saveButton;

        this.toolbar?.addEventListener("click", this.handleToolbarClick.bind(this));
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

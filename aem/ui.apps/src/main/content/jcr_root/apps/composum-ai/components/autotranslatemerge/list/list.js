const URL_MERGE_SERVLET = '/bin/cpm/ai/aitranslationmerge';
const PATH_CHOOSER_URL = '/mnt/overlay/cq/gui/content/linkpathfield/picker.html';

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
        this.linkModal = new AITranslateLinkEditModal();
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

    /** If the message is null, the error field is hidden. */
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
        this.rteContainer = row.querySelector(".rte-container");
        this.editor = this.rteContainer?.querySelector(".rte-editor") || row.querySelector(".text-editor");

        this.copyButton = this.actionrow.querySelector(".copy-to-editor");
        this.appendButton = this.actionrow.querySelector(".append-to-editor");
        this.mergeButton = this.actionrow.querySelector(".intelligent-merge");

        this.resetButton = this.actionrow.querySelector(".reset-editor");
        this.saveButton = this.actionrow.querySelector(".save-editor");

        if (this.rteContainer) {
            new AITranslatorMergeRTE(this.rteContainer, tool);
        }

        this.copyButton.addEventListener("click", this.copyToEditor.bind(this));
        if (this.appendButton) this.appendButton.addEventListener("click", this.appendToEditor.bind(this));
        this.mergeButton.addEventListener("click", this.intelligentMerge.bind(this));

        this.resetButton.addEventListener("click", this.resetEditor.bind(this));
        this.saveButton.addEventListener("click", this.saveEditor.bind(this));
    }

    copyToEditor() {
        this.editor.innerHTML = this.row.dataset.nt;
    }

    appendToEditor() {
        this.editor.innerHTML += this.row.dataset.nt;
    }

    resetEditor() {
        this.editor.innerHTML = this.row.dataset.e;
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
                    console.log("Merge successful");
                    this.tool.showError(null);
                })
                .catch(error => {
                    console.error("Error in intelligentMerge", error);
                    this.tool.showError(error.message);
                }).finally(() => {
                btn.disabled = false;
                btn.classList.remove('activespinner');
            });
        });
    }

    saveEditor() {
        const btn = this.saveButton;
        const row = this.row;
        this.tool.showError();
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
                    body: this.editor.value || this.editor.innerHTML
                })
            })
                .then(response => {
                    if (response.ok) {
                        return response.text();
                    } else {
                        return response.text().then(errMsg => {
                            throw new Error(errMsg);
                        });
                    }
                })
                .then(responseText => {
                    if (!responseText || !responseText.trim()) {
                        throw new Error();
                    }
                    let result = JSON.parse(responseText);
                    if (!result.saved) {
                        throw new Error(); // no error message to speak of
                    } else {
                        row.classList.add("merged");
                    }
                })
                .catch(error => {
                    console.error("Error in saveEditor", error);
                    this.tool.showError("Save failed. " + error?.message);
                }).finally(() => {
                btn.disabled = false;
                btn.classList.remove('activespinner');
            });
        });
    }
}

/** Manages the rich text editor functionalities, including toolbar actions and link management. */
class AITranslatorMergeRTE {
    constructor(container, tool) {
        this.tool = tool;
        this.editor = container.querySelector(".rte-editor") || container.querySelector(".text-editor");
        this.toolbar = container.querySelector(".rte-toolbar");

        this.toolbar?.addEventListener("click", this.handleToolbarClick.bind(this));

        this.initLinkHandlers();
    }

    handleToolbarClick(event) {
        const command = event.target.dataset.command;
        if (command) {
            document.execCommand(command, false, null);
            this.editor.focus(); // Refocus the editor after the action
        }
    }

    initLinkHandlers() {
        const editLinkButton = this.toolbar.querySelector('.edit-link-btn');
        const removeLinkButton = this.toolbar.querySelector('.remove-link-btn');

        editLinkButton.addEventListener("click", () => {
            const anchor = this.getSelectedAnchor();
            this.editLink(anchor);
        });

        removeLinkButton.addEventListener("click", () => {
            const anchor = this.getSelectedAnchor();
            if (anchor) {
                this.removeLink(anchor);
            } else {
                alert("No link is currently selected.");
            }
        });

    }

    getSelectedAnchor() {
        const selection = window.getSelection();
        if (!selection.rangeCount) return null;
        let element = selection.getRangeAt(0).startContainer;
        while (element && element.nodeType === Node.TEXT_NODE) {
            element = element.parentElement;
        }
        if (element && element.tagName.toLowerCase() === 'a') {
            return element;
        }
        return null;
    }

    editLink(anchor) {
        let selectedText = undefined;
        if (!anchor) {
            // if selection is within this.editor, save it
            const selection = window.getSelection();
            if (selection.rangeCount) {
                if (selection.baseNode.parentElement.closest('.rte-editor') === this.editor) {
                    this.savedRange = selection.getRangeAt(0);
                    selectedText = this.savedRange.toString();
                } else {
                    this.savedRange = null;
                }
            }
        }

        this.tool.linkModal.editLink(anchor, selectedText, this.saveLink.bind(this));
    }

    saveLink(anchor, linkModal) {
        if (!anchor) {
            if (this.savedRange) {
                const range = this.savedRange;
                const newAnchor = document.createElement('a');
                linkModal.updateAnchor(newAnchor);
                range.deleteContents();
                range.insertNode(newAnchor);

                const sel = window.getSelection();
                sel.removeAllRanges();
                sel.addRange(range);
            } else {
                console.error("Bug: No anchor or saved range to insert link into.");
            }
        } else {
            linkModal.updateAnchor(anchor);
        }
    }

    removeLink(anchor) {
        const range = document.createRange();
        const sel = window.getSelection();
        range.selectNodeContents(anchor);
        anchor.replaceWith(...anchor.childNodes);
        sel.removeAllRanges();
        sel.addRange(range);
    }
}

class AITranslateLinkEditModal {

    constructor() {
        this.modal = document.getElementById("edit-link-modal");
        this.inputAnchorText = this.modal.querySelector("#edit-anchor-text");
        this.inputHref = this.modal.querySelector("#edit-anchor-href");
        this.inputTitle = this.modal.querySelector("#edit-anchor-title");
        this.inputRel = this.modal.querySelector("#edit-anchor-rel");
        this.inputTarget = this.modal.querySelector("#edit-anchor-target"); // select
        this.saveLinkBtn = this.modal.querySelector("#save-link-btn");
        this.cancelLinkBtn = this.modal.querySelector("#cancel-link-btn");
        this.choosePathBtn = this.modal.querySelector("#choose-path-btn");

        this.pathChooser = new AITranslationPathChooser();
        this.choosePathBtn.addEventListener("click", () => this.pathChooser.openDialog(this.inputHref));

        this.saveLinkBtn.onclick = this.saveLink.bind(this);
        this.cancelLinkBtn.onclick = this.hideModal.bind(this);
    }

    editLink(anchor, selectedText, saveCallback) {
        this.saveCallback = saveCallback;
        this.anchor = anchor;
        // Pre-fill inputs with current anchor values if the anchor exists.
        if (anchor) {
            this.inputAnchorText.value = anchor.textContent;
            this.inputHref.value = anchor.getAttribute('href') || '';
            this.inputTitle.value = anchor.getAttribute('title') || '';
            this.inputRel.value = anchor.getAttribute('rel') || '';
            this.inputTarget.value = anchor.getAttribute('target') || '';
        } else {
            // Clear fields if no anchor is selected.
            this.inputAnchorText.value = selectedText || '';
            this.inputHref.value = '';
            this.inputTitle.value = '';
            this.inputRel.value = '';
            this.inputTarget.value = '';
        }
        this.showModal();
    }

    updateAnchor(anchor) {
        const newAnchorText = this.inputAnchorText.value;
        const newHref = this.inputHref.value;
        const newTitle = this.inputTitle.value;
        const newRel = this.inputRel.value;
        const newTarget = this.inputTarget.value;

        anchor.href = newHref;
        anchor.textContent = newAnchorText;
        if (newTitle) {
            anchor.setAttribute('title', newTitle);
        } else {
            anchor.removeAttribute('title');
        }
        if (newRel) {
            anchor.setAttribute('rel', newRel);
        } else {
            anchor.removeAttribute('rel');
        }
        if (newTarget) {
            anchor.setAttribute('target', newTarget);
        } else {
            anchor.removeAttribute('target');
        }
    }

    saveLink() {
        this.saveCallback(this.anchor, this);
        this.hideModal();
    }

    hideModal() {
        this.modal.classList.add("hidden");
    }

    showModal() {
        this.modal.classList.remove("hidden");
    }

}

class AITranslationPathChooser {

    constructor() {
        this.pathChooserContent = document.getElementById('path-chooser-content');
    }

    /** Loads PATH_CHOOSER_URL and inserts that into #path-chooser-content and removes class hidden from #path-chooser-modal */
    openDialog(pathInput, path) {
        this.removeDialog();
        this.pathInput = pathInput;
        const pathValue = path || this.pathInput.value;
        this.loadPath(pathValue, this.dialogSetup.bind(this));
    }

    loadPath(path, callback) {
        const url = path && path.startsWith('/') ? PATH_CHOOSER_URL + path : PATH_CHOOSER_URL;
        this.pathChooserContent.innerHTML = '';
        fetch(url)
            .then(response => response.text())
            .then(html => this.pathChooserContent.innerHTML = html)
            .then(() => callback())
            .catch(error => console.error("Error in choosePath", error));
    }

    dialogSetup() {
        this.pathDialog = this.pathChooserContent.querySelector('coral-dialog');
        this.pathDialog.show();
        this.pathDialog.on('coral-overlay:close', () => this.pathDialog.remove());
        this.pathDialog.on('click', '.granite-pickerdialog-submit', this.onSelect.bind(this));
        this.pathDialog.on('click', 'coral-columnview-item.is-selectable', this.selectItem.bind(this));
    }

    removeDialog() {
        if (this.pathDialog) {
            this.pathDialog.remove();
            this.pathDialog = null;
        }
    }

    onSelect() {
        const path = this.pathDialog.querySelector('coral-checkbox[checked]')?.parentNode?.dataset?.foundationCollectionItemId;
        this.pathDialog.remove();
        if (path) {
            this.pathInput.value = path;
        }
    }

    selectItem(event) {
        const path = event.target.dataset.foundationCollectionItemId;
        if (path) {
            this.openDialog(this.pathInput, path);
        }
    }

}

new AITranslateMergeTool();

console.log("AutoTranslateMerge list.js loaded");

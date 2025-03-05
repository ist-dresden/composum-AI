const URL_MERGE_SERVLET = '/bin/cpm/ai/aitranslationmerge';
const PATH_CHOOSER_URL = '/mnt/overlay/cq/gui/content/linkpathfield/picker.html';
const KEY_LOCALSTORAGE = 'composum.ai.autotranslatemerge';

/** Handles the general script functionality for the Translation Merge Tool. */
class AITranslateMergeTool {
    constructor() {
        document.addEventListener("DOMContentLoaded", () => {
            this.tableBody = document.querySelector(".propertiestable");
            this.initFooterButtons();
            this.reinitialize();
            this.readStateFromHistory();
        });
    }

    reinitialize() {
        console.log("init");
        this.initTableEventListeners();
        this.initNavButtons();
        document.querySelectorAll('coral-tooltip').forEach(tooltip => tooltip.delay = 1000);
        this.resizeTextAreas();
    }

    /** Initializes table event listeners for handling row-specific actions. */
    initTableEventListeners() {
        this.tableBody.querySelectorAll("tr.datarow").forEach(row => {
            if (row.initialized) return;
            this.initDatarow(row);
            row.initialized = true;
        });
        this.tableBody.querySelectorAll("tr.component-head").forEach(row => {
            if (row.initialized) return;
            this.initComponentHead(row)
            row.initialized = true;
        });
        this.linkModal = new AITranslateLinkEditModal();
    }

    initDatarow(row) {
        const id = row.dataset.id;
        const actionrow = document.getElementById("actionrow-" + id);
        const headerrow = document.getElementById("row-" + id);
        new AITranslateMergeRow(row, actionrow, headerrow, this);
    };

    initComponentHead(row) {
        new AIComponentRow(this, row);
    };

    /** Makes the textareas one line larger than the content. */
    resizeTextAreas() {
        this.tableBody.querySelectorAll('textarea').forEach(textarea => {
            textarea.style.height = 'auto';
            textarea.style.height = textarea.scrollHeight + 'px';
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
        this.propertySelect = document.querySelector('#propertyfilter');
        this.scopeSelect = document.querySelector('#scope');
        this.propertySelect.value = new URLSearchParams(window.location.search).get('propertyfilter') || 'allstati';
        this.scopeSelect.value = new URLSearchParams(window.location.search).get('scope') || 'unfinished';
        this.propertySelect.addEventListener('change', this.adaptFilters.bind(this));
        this.scopeSelect.addEventListener('change', this.adaptFilters.bind(this));
    }

    /** We save the state of the toggleDiffs and toggleCurrent to the local storage to be able to restore it. */
    saveStateToHistory() {
        const state = {
            showDiffs: document.body.classList.contains('hide-diffs'),
            showCurrent: document.body.classList.contains('show-currenttext')
        }
        localStorage.setItem(KEY_LOCALSTORAGE, JSON.stringify(state));
    }

    readStateFromHistory() {
        const stateRep = localStorage.getItem(KEY_LOCALSTORAGE);
        const state = stateRep ? JSON.parse(stateRep) : {};
        if (state.showDiffs) {
            this.toggleDiffs();
        }
        if (state.showCurrent) {
            this.toggleCurrent();
        }
    }

    toggleDiffs() {
        console.log(">>toggleDiffs", document.body.classList);
        document.body.classList.toggle('show-diffs');
        document.body.classList.toggle('hide-diffs');
        console.log("<<toggleDiffs", document.body.classList);
        this.saveStateToHistory();
    }

    toggleCurrent() {
        console.log(">>toggleCurrent", document.body.classList);
        document.body.classList.toggle('show-currenttext');
        document.body.classList.toggle('hide-currenttext');
        console.log("<<toggleCurrent", document.body.classList);
        this.saveStateToHistory();
    }

    /** Implements the selects for propertyfilter and scope by reloading the page with the according query parameters. */
    adaptFilters() {
        const propertyfilter = this.propertySelect.value;
        const scope = this.scopeSelect.value;
        const url = new URL(window.location.href);
        url.searchParams.set('propertyfilter', propertyfilter);
        url.searchParams.set('scope', scope);
        window.location.href = url.href;
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

    /** Makes a call to the AutoTranslateMergeServiceImpl */
    callOperation(button, operation, data, onSuccess) {
        this.showError(null);
        Granite.csrf.refreshToken().then(token => {
            button.disabled = true;
            button.classList.add('activespinner');
            fetch(URL_MERGE_SERVLET + "?operation=" + operation, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'CSRF-Token': token
                },
                body: JSON.stringify(data)
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
                    onSuccess(responseText);
                    this.showError(null);
                })
                .catch(error => {
                    console.error("Error in " + operation, error);
                    this.showError(error.message);
                }).finally(() => {
                button.disabled = false;
                button.classList.remove('activespinner');
            });
        });
    }

    /**
     * Reload the component: adds parameter componentpath and propertyname to the url, loads that,
     * puts that into a temporary space and replaces the tr tags in the table with these from that temporary space.
     * Annoyingly we cannot replace the whole document since the editors / text areas might have unsaved changes, and
     * it is also possible that rows vanish because of the filters.
     * Thus, we search for the existing component header, insert the new children before that, and remove it and the
     * other children.
     */
    reloadComponent(componentpath, propertyname) {
        const url = new URL(window.location.href);
        url.searchParams.set('propertyfilter', 'allstati'); // make sure it's visible
        url.searchParams.set('componentpath', componentpath);
        if (propertyname) url.searchParams.set('propertyname', propertyname);
        console.log('reloadComponent', url.href);
        const tableBody = document.querySelector(".propertytable tbody");
        var selector = `tr[data-componentpath="${componentpath}"]`;
        if (propertyname) {
            selector += `[data-cancelpropertyname="${propertyname}"]`;
        } else {
            selector += ':not([data-cancelpropertyname]';
        }
        const trs = tableBody.querySelectorAll(selector);
        if (!trs.length) {
            debugger;
        }
        fetch(url.href)
            .then(response => response.text())
            .then(html => {
                const tempDiv = document.createElement('div');
                tempDiv.innerHTML = html;
                const replacements = tempDiv.querySelectorAll(".propertytable tbody tr");
                const firsttr = trs[0];
                if (!firsttr) {
                    debugger;
                }
                replacements.forEach(tr => {
                    tableBody.insertBefore(tr, firsttr);
                });
                trs.forEach(tr => tr.remove());
                this.reinitialize();
            })
            .catch(error => {
                console.error("Error in reloadComponent", error);
                this.showError(error.message);
            });
    }

}

/** Handles the cancel and reinstate inheritance operation. */
class AIComponentRow {
    /** componentRow = first row displaying component */
    constructor(tool, componentHead) {
        if (componentHead.initialized) return;
        componentHead.initialized = true;
        this.tool = tool;
        this.componentHead = componentHead;
        this.cancelInheritanceButton = this.componentHead.querySelector(".cancelinheritance");
        this.reenableInheritanceButton = this.componentHead.querySelector(".reenableinheritance");
        this.cancelInheritanceButton.addEventListener("click", this.cancelInheritance.bind(this));
        this.reenableInheritanceButton.addEventListener("click", this.reenableInheritance.bind(this));
    }

    /** Calls the merge servlet with operation cancelInheritance and path and propertyName as POST parameters. */
    cancelInheritance() {
        this.cancelOrReenable(this.cancelInheritanceButton, 'cancelInheritance');
    }

    /** Calls the merge servlet with operation reenableInheritance and path and propertyName as POST parameters. */
    reenableInheritance() {
        this.cancelOrReenable(this.reenableInheritanceButton, 'reenableInheritance');
    }

    cancelOrReenable(button, operation) {
        const data = {
            path: this.componentHead.dataset.componentpath,
            propertyName: this.componentHead.dataset.cancelpropertyname
        };
        this.tool.callOperation(button, operation, data, responseText => {
            if (!responseText || !responseText.trim()) {
                throw new Error();
            }
            let result = JSON.parse(responseText);
            if (!result.done) {
                throw new Error(); // no error message to speak of
            } else {
                this.tool.reloadComponent(data.path, data.propertyName);
            }
        });
    }

}

/** Handles copy, save, and intelligent merge actions for each table row. */
class AITranslateMergeRow {
    constructor(row, actionrow, headerrow, tool) {
        if (row.initialized) return;
        row.initialized = true;
        this.row = row;
        this.actionrow = actionrow;
        this.headerrow = headerrow;
        this.tool = tool;
        this.rteContainer = row.querySelector(".rte-container");
        this.editor = this.rteContainer?.querySelector(".rte-editor") || row.querySelector(".text-editor");

        this.copyButton = this.actionrow.querySelector(".copy-to-editor");
        this.mergeButton = this.actionrow.querySelector(".intelligent-merge");
        this.acceptButton = this.actionrow.querySelector(".accept-translation");

        this.resetButton = this.actionrow.querySelector(".reset-editor");
        this.saveButton = this.actionrow.querySelector(".save-editor");

        if (this.rteContainer) {
            new AITranslatorMergeRTE(this.rteContainer, tool);
        }

        if (this.copyButton) this.copyButton.addEventListener("click", this.copyToEditor.bind(this));
        if (this.mergeButton) this.mergeButton.addEventListener("click", this.intelligentMerge.bind(this));
        if (this.acceptButton) this.acceptButton.addEventListener("click", this.acceptTranslation.bind(this));

        if (this.resetButton) this.resetButton.addEventListener("click", this.resetEditor.bind(this));
        if (this.saveButton) this.saveButton.addEventListener("click", this.saveEditor.bind(this));
    }

    setEditorValue(value) {
        if (this.editor.tagName === 'TEXTAREA') {
            this.editor.value = value;
        } else {
            this.editor.innerHTML = value;
        }
    }

    copyToEditor() {
        this.setEditorValue(this.row.dataset.nt || '');
    }

    resetEditor() {
        this.setEditorValue(this.row.dataset.e || '');
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
        this.tool.callOperation(this.mergeButton, 'merge', data, mergedText => {
            this.setEditorValue(mergedText);
            console.log("Merge successful");
        });
    }

    saveEditor() {
        const data = {
            path: this.row.dataset.path,
            propertyName: this.row.dataset.propertyname,
            body: this.editor.value || this.editor.innerHTML
        };
        this.tool.callOperation(this.saveButton, 'save', data, responseText => {
            if (!responseText || !responseText.trim()) {
                throw new Error();
            }
            let result = JSON.parse(responseText);
            if (!result.saved) {
                throw new Error(); // no error message to speak of
            } else {
                this.row.classList.add("processed");
                this.actionrow.classList.add("processed");
                this.headerrow.classList.add("processed");
                this.simulateButtonPress(this.saveButton);
            }
        });
    }

    /** Makes the button green for a second. */
    simulateButtonPress(button) {
        button.disabled = true;
        setTimeout(() => button.disabled = false, 1000);
    }

    /** Calls the merge servlet with operation acceptTranslation and path and propertyName as POST parameters. */
    acceptTranslation() {
        const data = {
            path: this.row.dataset.path,
            propertyName: this.row.dataset.propertyname
        }
        this.tool.callOperation(this.acceptButton, 'acceptTranslation', data, responseText => {
            if (!responseText || !responseText.trim()) {
                throw new Error();
            }
            let result = JSON.parse(responseText);
            if (!result.accepted) {
                throw new Error(); // no error message to speak of
            } else {
                this.row.classList.add("processed");
                this.actionrow.classList.add("processed");
                this.headerrow.classList.add("processed");
            }
        });
    }

}

/** Manages the rich text editor functionalities, including toolbar actions and link management. */
class AITranslatorMergeRTE {
    constructor(container, tool) {
        if (container.initialized) return;
        container.initialized = true;
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
                // set selection to the new anchor
                const newRange = document.createRange();
                newRange.selectNodeContents(newAnchor);
                sel.addRange(newRange);
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
            var href = anchor.getAttribute('href') || '';
            if (href.startsWith('/content/') && !href.startsWith('/content/dam/') && href.endsWith('.html')) {
                href = href.substring(0, href.length - 5);
            }
            this.inputHref.value = href;
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
        var newHref = this.inputHref.value;
        const newTitle = this.inputTitle.value;
        const newRel = this.inputRel.value;
        const newTarget = this.inputTarget.value;

        if (newHref && newHref.startsWith('/content/') && !newHref.startsWith('/content/dam') && !newHref.includes('.html')) {
            newHref += '.html';
        }
        anchor.href = newHref;
        anchor.textContent = newAnchorText;
        if (newTitle) {
            anchor.setAttribute('title', newTitle);
        } else {
            anchor.removeAttribute('title');
        }
        if (newRel) {
            anchor.setAttribute('rel', newRel);
        }
        if ('_blank' === newTarget) { // general guideline or new link
            anchor.setAttribute('rel', 'noopener noreferrer');
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
    async openDialog(pathInput, path) {
        this.removeDialog();
        this.pathInput = pathInput;
        const pathValue = path || this.pathInput.value || document.body.dataset?.pagePath;
        await this.loadPath(null);
        this.dialogSetup();
        if (pathValue) { // load all prefixes of pathValue
            const pathSplitted = pathValue.split('/').slice(1);
            let currentPath = '';
            for (let i = 0; i < pathSplitted.length; i++) {
                currentPath += '/' + pathSplitted[i];
                await this.loadPath(currentPath);
                this.mergeDialogs(currentPath);
            }
            this.selectPath(pathValue);
        }
    }

    /** Loads the column corresponding to the last element of the path. */
    async loadPath(path) {
        const url = path && path.startsWith('/') ? PATH_CHOOSER_URL + path : PATH_CHOOSER_URL;
        this.pathChooserContent.innerHTML = '';
        try {
            const response = await fetch(url);
            const html = await response.text();
            this.pathChooserContent.innerHTML = html;
        } catch (error) {
            console.error("Error in choosePath", error);
        }
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
        if (path) {
            this.pathInput.value = path;
        }
        this.removeDialog();
    }

    async selectItem(event) {
        const path = event?.target?.dataset?.foundationCollectionItemId;
        if (path) {
            await this.loadPath(path);
            this.mergeDialogs(path);
        }
    }

    /** Moves elements (new columns etc.) from this.pathChooserContent to this.pathDialog . */
    mergeDialogs(parentPath) {
        if (!this.pathDialog || !this.pathChooserContent) return;
        // move element .granite-pickerdialog-titlebar
        const titlebar = this.pathChooserContent.querySelector('.granite-pickerdialog-titlebar');
        const dialogTitlebar = this.pathDialog.querySelector('.granite-pickerdialog-titlebar');
        if (titlebar && dialogTitlebar) {
            dialogTitlebar.replaceWith(titlebar);
        }
        // find within the coral-columnview the columnview-column that has no coral-wait in it and overwrite / put that
        // into the coral-columnview in this.pathDialog
        const columnview = this.pathChooserContent.querySelector('coral-columnview');
        const dialogColumnview = this.pathDialog.querySelector('coral-columnview');
        // merge the children of columnview and dialogColumnView into dialogColumnview. Each child of columnview
        // is checked whether it has a coral-wait element somewhere, and if not the corresponding child in dialogColumview
        // is replaced, or it is added there.
        const columnviewChildren = Array.from(columnview.childNodes);
        const dialogColumnviewChildren = Array.from(dialogColumnview.childNodes);
        for (let i = 0; i < columnviewChildren.length; i++) {
            const child = columnviewChildren[i];
            if (child.querySelector('coral-columnview-column-content coral-wait')) {
                continue;
            }
            if (i < dialogColumnviewChildren.length) {
                dialogColumnviewChildren[i].replaceWith(child);
            } else {
                dialogColumnview.appendChild(child);
            }
        }
        while (dialogColumnview.childNodes.length > columnviewChildren.length) {
            dialogColumnview.childNodes[dialogColumnview.childNodes.length - 1].remove();
        }
        dialogColumnview.childNodes[dialogColumnview.childNodes.length - 1].scrollIntoView({
            behavior: 'smooth',
            block: 'center',
            inline: 'end'
        });
        this.pathChooserContent.innerHTML = '';
        const parent = this.pathDialog.querySelector(`coral-columnview-item[data-foundation-collection-item-id="${parentPath}"]`);
        if (parent) {
            parent.active = true;
        }
    }

    /** Finds the coral-columnview-item by the data-foundation-collection-item-id and checks that and scrolls it into view. */
    selectPath(path) {
        if (path) {
            const item = this.pathDialog.querySelector(`coral-columnview-item[data-foundation-collection-item-id="${path}"]`);
            if (item) {
                item.selected = true;
                item.scrollIntoView({behavior: 'smooth', block: 'center', inline: 'end'});
            }
        }
    }

}

new AITranslateMergeTool();

console.log("AutoTranslateMerge list.js loaded");

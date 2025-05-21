/**
 * Class representing the Bulk Replace application.
 */
class BulkReplaceApp {

    static formStateKey = 'aem-composumAI-bulkedit-formstate';
    static replacementHistoryKey = 'aem-composumAI-bulkedit-replaced';

    /**
     * Constructs the BulkReplaceApp instance and initializes DOM elements.
     */
    constructor() {
        this.cacheDomElements();
        this.bindEvents();
        this.loadSavedSettings();
        this.initTooltips();
        this.currentSearchJobId = null;
    }

    /**
     * Initializes the BulkReplaceApp instance.
     */
    static initApp() {
        new BulkReplaceApp();
    }

    /**
     * Caches frequently accessed DOM elements.
     */
    cacheDomElements() {
        this.searchBtn = document.getElementById("search-btn");
        this.replaceBtn = document.getElementById("replace-btn");
        this.progressBar = document.querySelector(".progress-bar");
        this.tableBody = document.querySelector("table tbody");
        this.rootPageInput = document.getElementById("root-page");
        this.searchStringInput = document.getElementById("search-string");
        this.replacementInput = document.getElementById("replacement-string");
        this.createVersionCheckbox = document.getElementById("create-version");
        this.autoPublishCheckbox = document.getElementById("auto-publish");
        this.clearFormBtn = document.getElementById("clear-form-btn");
        this.exportHistoryBtn = document.getElementById("export-history-btn");
        this.clearHistoryBtn = document.getElementById("clear-history-btn");
    }

    /**
     * Binds event handlers to DOM elements.
     */
    bindEvents() {
        this.searchBtn.addEventListener("click", this.handleSearchClick.bind(this));
        this.replaceBtn.addEventListener("click", this.handleReplaceClick.bind(this));
        this.clearFormBtn.addEventListener("click", this.handleClearForm.bind(this));
        this.exportHistoryBtn.addEventListener("click", this.handleExportHistory.bind(this));
        this.clearHistoryBtn.addEventListener("click", this.handleClearHistory.bind(this));
        // Bind header checkbox to select/deselect all property checkboxes, then update states.
        document.getElementById("select-all").addEventListener("change", (event) => {
            const checked = event.target.checked;
            document.querySelectorAll("input.select-property").forEach(cb => cb.checked = checked);
            this.updateIndeterminateStates();
        });
        // Delegate change events within the table body.
        this.tableBody.addEventListener("change", (event) => {
            if (event.target.classList.contains("select-page")) {
                // Toggle all property checkboxes of this page.
                const page = event.target.closest("tr").getAttribute("data-page");
                const checked = event.target.checked;
                document.querySelectorAll(`tr[data-page="${page}"] input.select-property`).forEach(cb => cb.checked = checked);
            }
            this.updateIndeterminateStates();
        });
    }

    /**
     * Updates the indeterminate state of header and page checkboxes based on selection.
     */
    updateIndeterminateStates() {
        // Update global "select-all" checkbox state.
        const allProps = document.querySelectorAll("input.select-property");
        const total = allProps.length;
        const checkedCount = Array.from(allProps).filter(cb => cb.checked).length;
        const selectAll = document.getElementById("select-all");
        if (checkedCount === 0) {
            selectAll.checked = false;
            selectAll.indeterminate = false;
        } else if (checkedCount === total) {
            selectAll.checked = true;
            selectAll.indeterminate = false;
        } else {
            selectAll.checked = false;
            selectAll.indeterminate = true;
        }

        // Update each page header checkbox.
        document.querySelectorAll("input.select-page").forEach(pageCb => {
            const page = pageCb.closest('tr').getAttribute("data-page");
            const props = document.querySelectorAll(`tr[data-page="${page}"] input.select-property`);
            const totalProps = props.length;
            const checkedProps = Array.from(props).filter(cb => cb.checked).length;
            if (checkedProps === 0) {
                pageCb.checked = false;
                pageCb.indeterminate = false;
            } else if (checkedProps === totalProps) {
                pageCb.checked = true;
                pageCb.indeterminate = false;
            } else {
                pageCb.checked = false;
                pageCb.indeterminate = true;
            }
        });
    }

    /**
     * Loads saved input settings from localStorage.
     */
    loadSavedSettings() {
        const saved = localStorage.getItem(BulkReplaceApp.formStateKey);
        if (saved) {
            try {
                const settings = JSON.parse(saved);
                if (settings.root) {
                    this.rootPageInput.value = settings.root;
                }
                if (settings.search) {
                    this.searchStringInput.value = settings.search;
                }
                if (settings.replacement) {
                    this.replacementInput.value = settings.replacement;
                }
            } catch (e) {
                console.error("Error parsing saved settings", e);
            }
        }
    }

    /**
     * Saves current input settings to localStorage.
     */
    saveSettings() {
        const settings = {
            root: this.rootPageInput.value.trim(),
            search: this.searchStringInput.value,
            replacement: this.replacementInput.value
            // Do not save createVersion and autoPublish
        };
        localStorage.setItem(BulkReplaceApp.formStateKey, JSON.stringify(settings));
    }

    /**
     * Retrieves the CSRF token via a network call.
     *
     * @returns a Promise resolving with the CSRF token string.
     */
    getCSRFToken() {
        return fetch("/libs/granite/csrf/token.json")
            .then(response => response.json())
            .then(data => data.token);
    }

    /**
     * Displays a bootstrap error alert with the provided message.
     *
     * @param message the error message to display
     */
    showErrorAlert(message) {
        const errorAlert = document.getElementById("error-alert");
        if (errorAlert) {
            errorAlert.textContent = message;
            errorAlert.style.display = "block";
        }
    }

    /**
     * Handles the click event for the search button.
     */
    handleSearchClick() {
        this.saveSettings();
        // Hide any existing error alert.
        const errorAlert = document.getElementById("error-alert");
        if (errorAlert) { errorAlert.style.display = "none"; }
        const root = this.rootPageInput.value.trim();
        const term = this.searchStringInput.value;
        if (!root || !term) {
            this.showErrorAlert("Please provide both root page and search string.");
            return;
        }
        this.tableBody.innerHTML = "";
        this.progressBar.style.width = "50%";
        this.progressBar.textContent = "50%";
        this.replaceBtn.disabled = true;
        this.startSearchJob(root, term);
    }

    /**
     * Starts a search job by posting parameters and attaching an EventSource.
     *
     * @param root the root page path
     * @param term the search term
     */
    startSearchJob(root, term) {
        const formData = new URLSearchParams();
        formData.append("operation", "search");
        formData.append("rootPath", root);
        formData.append("term", term);

        this.getCSRFToken()
            .then(token => {
                return fetch("/bin/cpm/ai/bulkreplace", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded",
                        "CSRF-Token": token
                    },
                    body: formData.toString()
                });
            })
            .then(this.handleJobResponse.bind(this))
            .then((data) => {
                this.currentSearchJobId = data.jobId;
                this.attachEventSource("search", this.currentSearchJobId);
            })
            .catch(this.handleError.bind(this));
    }

    /**
     * Processes the job response.
     *
     * @param response the response object
     * @returns a Promise resolving to JSON data if the response is accepted
     */
    async handleJobResponse(response) {
        if (response.status === 202) {
            return response.json();
        }
        throw new Error("Search could not be started. " + await response.text());
    }

    /**
     * Attaches an EventSource to handle server-sent events for a specified job.
     *
     * @param operation the operation type ("search" or "replace")
     * @param jobId     the job identifier
     */
    attachEventSource(operation, jobId) {
        // Only the search operation uses an event stream.
        if (operation === "search") {
            const evtSource = new EventSource("/bin/cpm/ai/bulkreplace?operation=" + operation + "&jobId=" + jobId);
            evtSource.addEventListener("page", this.handlePageEvent.bind(this));
            evtSource.addEventListener("summary", (event) => {
                this.progressBar.style.width = "100%";
                this.progressBar.textContent = "100%";
                this.replaceBtn.disabled = false;
                evtSource.close();
            });
            evtSource.onerror = (e) => {
                console.error("EventSource error:", e);
                this.showErrorAlert('Error during replacement: ' + e.message);
                evtSource.close();
            };
        }
        // Remove event stream handling for replace.
    }

    /**
     * Handles page events received via EventSource. This is called when a page is found.
     *
     * @param event the event containing page data
     */
    handlePageEvent(event) {
        const data = JSON.parse(event.data);
        const headerRow = document.createElement("tr");
        headerRow.classList.add("table-secondary");
        headerRow.setAttribute("data-page", data.page);
        if (data.matches) {
            headerRow.innerHTML = `
                <td><input type="checkbox" class="select-page"></td>
                <td colspan="3">
                    <i class="bi publishicon"></i>
                  <strong><a href="/editor.html${data.page}.html" target="_blank">${data.page}</a></strong> (${data.matches.length} matches)
                </td>
            `;
            this.tableBody.appendChild(headerRow);
            for (let i = 0; i < data.matches.length; i++) {
                const match = data.matches[i];
                const propRow = document.createElement("tr");
                propRow.setAttribute("data-page", data.page);
                propRow.setAttribute("data-component", match.componentPath);
                propRow.setAttribute("data-property", match.property);
                let componentLink = '';
                if (match.componentPath === 'jcr:content') {
                    componentLink = `<a class="small-component" href="/mnt/overlay/wcm/core/content/sites/properties.html?item=${data.page}" target="_blank">${match.componentPath}</a>`;
                } else {
                    componentLink = `<a class="small-component" href="/editor.html${data.page}.html#scrolltocomponent-${match.componentPath}" target="_blank">${match.componentPath}</a>`;
                }
                propRow.innerHTML = `
                    <td><input type="checkbox" class="select-property" checked></td>
                    <td>${componentLink}</td>
                    <td>${match.property}</td>
                    <td class="fill-excerpt">${match.excerpt}</td>
                `;
                this.tableBody.appendChild(propRow);
            }
        } else if (data.changed) {
            headerRow.classList.add("replaced-row");
            headerRow.innerHTML = `
                <td colspan="4"><strong>Replaced on page:</strong> ${data.page} (${data.changed.length} properties)</td>
            `;
            this.tableBody.appendChild(headerRow);
        }
        this.updateIndeterminateStates();
    }

    /**
     * Marks the table rows corresponding to changed properties on the given page.
     * Updates each affected row's excerpt with the new text and removes its checkbox.
     *
     * @param {string} page - The page path.
     * @param {Array} changedArr - Array of changed objects from the response.
     */
    markPageAsReplaced(page, changedArr, published) {
        document.querySelectorAll(`tr[data-page="${page}"] .publishicon`).forEach( publishicon => {
            if (published) {
                publishicon.classList.add("bi-check-circle");
            } else if (published === false) {
                publishicon.classList.add("bi-dash-circle");
            }
        })

        changedArr.forEach(changed => {
            // Locate the row with matching page, componentPath, and property.
            const selector = `tr[data-page="${page}"][data-component="${changed.componentPath}"][data-property="${changed.property}"]`;
            const row = document.querySelector(selector);
            if (row) {
                row.classList.add("replaced-row");
                // Remove checkbox from this row.
                row.querySelectorAll("input[type='checkbox']").forEach(cb => cb.remove());
                // Update the text excerpt cell (assumed to be the 4th cell) with the new excerpt.
                const cells = row.getElementsByTagName("td");
                cells[3].innerHTML = changed.excerpt;
            }
        });
    }

    /**
     * Handles the click event for the replace button, processing pages sequentially.
     */
    async handleReplaceClick() {
        this.saveSettings();
        // Hide any existing error alert.
        const errorAlert = document.getElementById("error-alert");
        if (errorAlert) { errorAlert.style.display = "none"; }
        const root = this.rootPageInput.value.trim();
        const term = this.searchStringInput.value;
        const replacement = this.replacementInput.value;
        if (!root || !term || replacement === null) {
            this.showErrorAlert("Please provide root page, search string and replacement.");
            return;
        }
        const selectedCheckboxes = document.querySelectorAll("input.select-property:checked");
        if (selectedCheckboxes.length === 0) {
            this.showErrorAlert("No properties selected for replacement.");
            return;
        }
        // Group targets per page
        const pageMap = {};
        selectedCheckboxes.forEach(checkbox => {
            const row = checkbox.closest("tr");
            const page = row.getAttribute("data-page");
            const component = row.getAttribute("data-component");
            const property = row.getAttribute("data-property");
            if (!pageMap[page]) {
                pageMap[page] = [];
            }
            pageMap[page].push({componentPath: component, property: property});
        });
        const pages = Object.keys(pageMap);
        let completed = 0;
        this.progressBar.style.width = "0%";
        this.progressBar.textContent = "0%";
        // Process each page sequentially using async/await
        for (const page of pages) {
            try {
                // Capture the ReplacePageResponse from the server.
                const res = await this.replaceForPage(
                    page,
                    term,
                    replacement,
                    pageMap[page],
                    this.createVersionCheckbox.checked,
                    this.autoPublishCheckbox.checked
                );
                console.log("Replace response for page:", page, res);

                // append data to history
                const history = JSON.parse(localStorage.getItem(BulkReplaceApp.replacementHistoryKey) || "[]");
                history.push(res);
                localStorage.setItem(BulkReplaceApp.replacementHistoryKey, JSON.stringify(history));

                // Use the response: if changed properties are reported, update the UI.
                if (res && res.changed && res.changed.length > 0) {
                    this.markPageAsReplaced(page, res.changed, res.published);
                }
                completed++;
                const progress = Math.round((completed / pages.length) * 100);
                this.progressBar.style.width = progress + "%";
                this.progressBar.textContent = progress + "%";
            } catch (error) {
                this.handleError(error);
            }
        }
    }

    /**
     * Starts the replacement job for a specific page.
     *
     * @param page         the page path
     * @param term         the search term
     * @param replacement  the replacement text
     * @param targets      an array of target objects
     * @param createVersion whether to create a version before replacement
     * @param autoPublish  whether to auto-publish the page after replacement
     * @returns a Promise resolving to the JSON response from the server.
     */
    replaceForPage(page, term, replacement, targets, createVersion, autoPublish) {
        return this.getCSRFToken().then(token => {
            return fetch("/bin/cpm/ai/bulkreplace?operation=replace", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "CSRF-Token": token
                },
                body: JSON.stringify({
                    page: page,
                    term: term,
                    replacement: replacement,
                    targets: targets,
                    createVersion: createVersion,
                    autoPublish: autoPublish
                })
            }).then(async response => {
                if (!response.ok) {
                    throw new Error("Replace operation failed for " + page + ": " + await response.text());
                }
                const text = await response.text();
                try {
                    return JSON.parse(text);
                } catch (e) {
                    throw new Error("Error during replacement: " + text);
                }
            });
        });
    }

    /**
     * Processes errors by displaying a bootstrap error alert.
     *
     * @param error the error object
     */
    handleError(error) {
        console.log("Error:", error);
        this.showErrorAlert(error.message);
    }

    /**
     * Initializes Bootstrap tooltips on the page.
     */
    initTooltips() {
        // Initialize Bootstrap tooltips for all elements with data-toggle="tooltip"
        $(function () {
            $('[data-toggle="tooltip"]').tooltip({delay: 1000});
        });
    }

    /**
     * Clears the form and removes saved form state.
     */
    handleClearForm() {
        this.rootPageInput.value = "";
        this.searchStringInput.value = "";
        this.replacementInput.value = "";
        this.createVersionCheckbox.checked = true;
        this.autoPublishCheckbox.checked = false;
        localStorage.removeItem(BulkReplaceApp.formStateKey);
    }

    /**
     * Exports the replacement history stored in localStorage as CSV.
     */
    handleExportHistory() {
        const history = JSON.parse(localStorage.getItem(BulkReplaceApp.replacementHistoryKey) || "[]");
        if (history.length === 0) {
            this.showErrorAlert("No history to export.");
            return;
        }
        let csvContent = `"Page","ComponentPath","Property","Published","Excerpt","OldValue","NewValue"\n`;
        // Helper to escape double quotes in CSV field values.
        const escapeCSV = (value) => {
            if (value == null) return "";
            var escapedVal = String(value).replace(/"/g, '""');
            escapedVal = escapedVal.replace(/\n/g, '\\n');
            return escapedVal;
        };
        history.forEach(entry => {
            if (entry.page && entry.changed) {
                const published = entry.published ? "true" : entry.published === false ? "false" : "";
                entry.changed.forEach(ch => {
                    csvContent += `"${escapeCSV(entry.page)}","${escapeCSV(ch.componentPath)}",` +
                    `"${escapeCSV(ch.property)}",${published},"${escapeCSV(ch.excerpt)}",` +
                    `"${escapeCSV(ch.oldValue)}","${escapeCSV(ch.newValue)}"\n`;
                });
            }
        });
        const blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = "bulkreplace_history.csv";
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }

    /**
     * Clears the replacement history from localStorage.
     */
    handleClearHistory() {
        localStorage.removeItem(BulkReplaceApp.replacementHistoryKey);
    }
}

/**
 * Handles the DOMContentLoaded event to initialize the application.
 */
function domContentLoadedHandler() {
    BulkReplaceApp.initApp();
}

document.addEventListener("DOMContentLoaded", domContentLoadedHandler);


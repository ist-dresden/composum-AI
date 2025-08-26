/**
 * IndexedDB wrapper for managing bulk replace history.
 * Provides better storage capacity than localStorage (50MB+ vs 5-10MB).
 */
class BulkReplaceDB {
    constructor() {
        this.dbName = 'BulkReplaceDB';
        this.version = 1;
        this.storeName = 'replacementHistory';
        this.db = null;
        this.isAvailable = false;
    }

    /**
     * Initialize the database connection and create schema if needed.
     * @returns {Promise} Resolves when database is ready
     */
    async init() {
        if (!window.indexedDB) {
            return Promise.reject('IndexedDB not supported');
        }

        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.dbName, this.version);
            
            request.onerror = () => {
                console.error('Failed to open IndexedDB:', request.error);
                reject(request.error);
            };
            
            request.onsuccess = (event) => {
                this.db = event.target.result;
                this.isAvailable = true;
                resolve(this.db);
            };
            
            request.onupgradeneeded = (event) => {
                const db = event.target.result;
                
                // Create object store if it doesn't exist
                if (!db.objectStoreNames.contains(this.storeName)) {
                    const store = db.createObjectStore(this.storeName, { 
                        keyPath: 'id', 
                        autoIncrement: true 
                    });
                    
                    // Create indexes for efficient queries
                    store.createIndex('date', 'date', { unique: false });
                    store.createIndex('page', 'page', { unique: false });
                }
            };
        });
    }

    /**
     * Add a replacement record to the database.
     * @param {Object} data - The replacement data to store
     * @returns {Promise} Resolves when data is stored
     */
    async addRecord(data) {
        if (!this.isAvailable) {
            throw new Error('Database not available');
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.storeName], 'readwrite');
            const store = transaction.objectStore(this.storeName);
            const request = store.add(data);
            
            request.onsuccess = () => resolve(request.result);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Get all records from the database.
     * @returns {Promise<Array>} Array of all stored records
     */
    async getAllRecords() {
        if (!this.isAvailable) {
            return [];
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.storeName], 'readonly');
            const store = transaction.objectStore(this.storeName);
            const request = store.getAll();
            
            request.onsuccess = () => resolve(request.result || []);
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Clear all records from the database.
     * @returns {Promise} Resolves when all records are cleared
     */
    async clearAll() {
        if (!this.isAvailable) {
            return Promise.resolve();
        }

        return new Promise((resolve, reject) => {
            const transaction = this.db.transaction([this.storeName], 'readwrite');
            const store = transaction.objectStore(this.storeName);
            const request = store.clear();
            
            request.onsuccess = () => {
                console.log('IndexedDB history cleared');
                resolve();
            };
            request.onerror = () => reject(request.error);
        });
    }

    /**
     * Get the approximate size of stored data.
     * @returns {Promise<number>} Size in bytes
     */
    async getStorageSize() {
        if (!this.isAvailable) {
            return 0;
        }

        const records = await this.getAllRecords();
        const blob = new Blob([JSON.stringify(records)]);
        return blob.size;
    }

    /**
     * Check if storage is approaching 150MB limit and cleanup oldest records if needed.
     * Called after bulk replacement operations complete.
     * @returns {Promise<boolean>} Returns true if cleanup was performed
     */
    async maintainStorageLimits() {
        if (!this.isAvailable) {
            return false;
        }

        const maxSizeBytes = 150 * 1024 * 1024; // 150MB limit
        const targetSizeAfterCleanup = 120 * 1024 * 1024; // Clean down to 120MB to leave room
        
        const currentSize = await this.getStorageSize();
        
        console.log(`IndexedDB storage: ${(currentSize / 1024 / 1024).toFixed(2)}MB used of ${(maxSizeBytes / 1024 / 1024).toFixed(0)}MB limit`);
        
        if (currentSize >= maxSizeBytes) {
            console.warn(`Storage limit reached (${(currentSize / 1024 / 1024).toFixed(2)}MB), performing cleanup...`);
            
            const records = await this.getAllRecords();
            if (records.length === 0) {
                console.warn('No records to clean up');
                return false;
            }
            
            // Sort by date (oldest first)
            records.sort((a, b) => new Date(a.date) - new Date(b.date));
            
            // Calculate how many records to remove to get below target size
            let sizeRemoved = 0;
            let recordsToRemove = [];
            
            for (const record of records) {
                const recordSize = new Blob([JSON.stringify(record)]).size;
                sizeRemoved += recordSize;
                recordsToRemove.push(record.id);
                
                // Stop when we've removed enough to get below target
                if (currentSize - sizeRemoved <= targetSizeAfterCleanup) {
                    break;
                }
            }
            
            // Remove the oldest records
            if (recordsToRemove.length > 0) {
                const transaction = this.db.transaction([this.storeName], 'readwrite');
                const store = transaction.objectStore(this.storeName);
                
                for (const id of recordsToRemove) {
                    store.delete(id);
                }
                
                console.log(`Removed ${recordsToRemove.length} oldest records to free up ${(sizeRemoved / 1024 / 1024).toFixed(2)}MB`);
                return true;
            }
        }
        
        return false;
    }
}

/**
 * Class representing the Bulk Replace application.
 */
class BulkReplaceApp {

    static formStateKey = 'aem-composumAI-bulkedit-formstate';
    // Removed replacementHistoryKey - no longer using localStorage for history

    /**
     * Constructs the BulkReplaceApp instance and initializes DOM elements.
     */
    constructor() {
        this.cacheDomElements();
        this.bindEvents();
        this.loadSavedSettings();
        this.initTooltips();
        this.currentSearchJobId = null;
        
        // Initialize IndexedDB for history storage
        this.historyDB = new BulkReplaceDB();
        this.historyDB.init().then(() => {
            // Update storage status after DB is ready
            this.updateStorageStatus();
        }).catch(error => {
            console.error('IndexedDB initialization failed, history will be disabled:', error);
            this.historyDB = null; // History disabled if IndexedDB not available
            this.updateStorageStatus(); // Will hide the storage status
        });
    }

    /**
     * Initializes the BulkReplaceApp instance.
     */
    static initApp() {
        const app = new BulkReplaceApp();
        return app;
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
        this.continueMsg = document.getElementById('continue-msg');
        this.storageStatusDiv = document.getElementById('storage-status');
        this.storageStatusText = document.getElementById('storage-status-text');
    }

    /**
     * Binds event handlers to DOM elements.
     */
    bindEvents() {
        this.searchBtn.addEventListener("click", this.handleSearchClick.bind(this));
        this.replaceBtn.addEventListener("click", this.handleReplaceClick.bind(this));
        this.clearFormBtn.addEventListener("click", this.handleClearForm.bind(this));
        this.exportHistoryBtn.addEventListener("click", () => this.handleExportHistory());
        this.clearHistoryBtn.addEventListener("click", () => this.handleClearHistory());
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
        if (errorAlert) {
            errorAlert.style.display = "none";
        }
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
        this.toggleContinueMsg(true);
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
                // Store occurrence count for validation during replace (-1 means not set)
                propRow.setAttribute("data-occurrences", match.occurrenceCount !== undefined ? match.occurrenceCount : -1);
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
        document.querySelectorAll(`tr[data-page="${page}"] .publishicon`).forEach(publishicon => {
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
     * Marks rows as skipped with yellow background for properties that couldn't be replaced.
     * @param {string} page - The page path.
     * @param {Array} skippedArr - Array of skipped target objects from the response.
     */
    markAsSkipped(page, skippedArr) {
        skippedArr.forEach(skipped => {
            // Locate the row with matching page, componentPath, and property.
            const selector = `tr[data-page="${page}"][data-component="${skipped.componentPath}"][data-property="${skipped.property}"]`;
            const row = document.querySelector(selector);
            if (row) {
                row.classList.add("skipped-row");
                // Remove checkbox from skipped rows (like replaced rows)
                row.querySelectorAll("input[type='checkbox']").forEach(cb => cb.remove());
            }
        });
    }

    /**
     * Handles the click event for the replace button, processing pages sequentially.
     */
    async handleReplaceClick() {
        this.saveSettings();
        this.toggleContinueMsg(false);
        // Hide any existing error alert.
        const errorAlert = document.getElementById("error-alert");
        if (errorAlert) {
            errorAlert.style.display = "none";
        }
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
            const occurrences = parseInt(row.getAttribute("data-occurrences") || "-1", 10);
            if (!pageMap[page]) {
                pageMap[page] = [];
            }
            pageMap[page].push({
                componentPath: component, 
                property: property,
                expectedOccurrences: occurrences
            });
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
                res.date = new Date().toISOString();

                // Save to IndexedDB if available
                if (this.historyDB && this.historyDB.isAvailable) {
                    try {
                        await this.historyDB.addRecord(res);
                    } catch (dbError) {
                        console.error("Failed to save to IndexedDB:", dbError);
                    }
                }

                // Use the response: if changed properties are reported, update the UI.
                if (res && res.changed && res.changed.length > 0) {
                    this.markPageAsReplaced(page, res.changed, res.published);
                }
                // Mark skipped properties with yellow background
                if (res && res.skipped && res.skipped.length > 0) {
                    this.markAsSkipped(page, res.skipped);
                }
                completed++;
                const progress = Math.round((completed / pages.length) * 100);
                this.progressBar.style.width = progress + "%";
                this.progressBar.textContent = progress + "%";
            } catch (error) {
                this.handleError(error);
            }
        }
        
        // Check storage limits after bulk operation completes
        if (completed > 0 && this.historyDB && this.historyDB.isAvailable) {
            await this.historyDB.maintainStorageLimits();
            
            // Update storage status display
            await this.updateStorageStatus();
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
     * Exports the replacement history stored in IndexedDB as CSV.
     */
    async handleExportHistory() {
        if (!this.historyDB || !this.historyDB.isAvailable) {
            this.showErrorAlert("History is not available (IndexedDB not supported).");
            return;
        }
        
        const history = await this.historyDB.getAllRecords();
        if (history.length === 0) {
            this.showErrorAlert("No history to export.");
            return;
        }
        let csvContent = `"Date","Page","ComponentPath","Property","Published","Excerpt","OldValue","NewValue","Skipped","Editor"\n`;
        // Helper to escape double quotes and newlines in CSV field values.
        const escapeCSV = (value) => {
            if (value == null) return "";
            var escapedVal = String(value).replace(/"/g, '""');
            escapedVal = escapedVal.replace(/\r?\n/g, ' ');
            return escapedVal;
        };
        history.forEach(entry => {
            const published = entry.published ? "true" : entry.published === false ? "false" : "";
            const editorUrl = location.href.replace(/\/apps.*/, '/editor.html') +
                entry.page + '.html';
            
            // Export changed items (not skipped)
            if (entry.page && entry.changed) {
                entry.changed.forEach(ch => {
                    csvContent += `"${entry.date.replace('T', ' ').slice(0, 19)}",` +
                        `"${escapeCSV(entry.page)}","${escapeCSV(ch.componentPath)}",` +
                        `"${escapeCSV(ch.property)}",${published},"${escapeCSV(ch.excerpt)}",` +
                        `"${escapeCSV(ch.oldValue)}","${escapeCSV(ch.newValue)}","",` +  // Empty skipped column
                        `"${escapeCSV(editorUrl)}"\n`;
                });
            }
            
            // Export skipped items
            if (entry.page && entry.skipped) {
                entry.skipped.forEach(sk => {
                    csvContent += `"${entry.date.replace('T', ' ').slice(0, 19)}",` +
                        `"${escapeCSV(entry.page)}","${escapeCSV(sk.componentPath)}",` +
                        `"${escapeCSV(sk.property)}",${published},"${escapeCSV(sk.excerpt || "")}",` +
                        `"${escapeCSV(sk.oldValue || "")}","","skipped",` +  // oldValue filled, newValue empty, marked as skipped
                        `"${escapeCSV(editorUrl)}"\n`;
                });
            }
        });
        const blob = new Blob([csvContent], {type: 'text/csv;charset=utf-8;'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `bulkreplace_history_${new Date().toISOString().replace('T', ' ').slice(0, 19)}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
    }

    /**
     * Clears the replacement history from IndexedDB.
     */
    async handleClearHistory() {
        if (!this.historyDB || !this.historyDB.isAvailable) {
            return;
        }
        
        await this.historyDB.clearAll();
        
        // Update storage status after clearing
        await this.updateStorageStatus();
    }

    toggleContinueMsg(status) {
        if (status) { // show it
            this.continueMsg.style.display = 'block';
        } else { // hide it
            this.continueMsg.style.display = 'none';
        }
    }

    /**
     * Updates the storage status display with current usage and warnings.
     */
    async updateStorageStatus() {
        if (!this.historyDB || !this.historyDB.isAvailable) {
            this.storageStatusDiv.style.display = 'none';
            return;
        }
        
        try {
            const sizeBytes = await this.historyDB.getStorageSize();
            const sizeMB = sizeBytes / 1024 / 1024;
            const sizeMBFormatted = sizeMB.toFixed(2);
            
            if (sizeMB >= 130) {
                // Critical - approaching cleanup threshold
                this.storageStatusDiv.className = 'alert alert-danger';
                this.storageStatusText.innerHTML = `<strong>Current storage: ${sizeMBFormatted} MB</strong> - Automatic history cleanup will occur at 150 MB. Please export history and clear it manually.`;
            } else if (sizeMB >= 100) {
                // Warning - getting full
                this.storageStatusDiv.className = 'alert alert-warning';
                this.storageStatusText.innerHTML = `<strong>Current storage: ${sizeMBFormatted} MB</strong> - History will be cleaned when it reaches 150 MB. Consider exporting your history and clearing it manually.`;
            } else {
                // Normal - show storage info
                this.storageStatusDiv.className = 'alert alert-info';
                this.storageStatusText.innerHTML = `Current storage: ${sizeMBFormatted} MB`;
            }
            
            this.storageStatusDiv.style.display = 'block';
        } catch (error) {
            console.error('Failed to update storage status:', error);
            this.storageStatusDiv.style.display = 'none';
        }
    }

}

/**
 * Handles the DOMContentLoaded event to initialize the application.
 */
function domContentLoadedHandler() {
    BulkReplaceApp.initApp();
}

document.addEventListener("DOMContentLoaded", domContentLoadedHandler);


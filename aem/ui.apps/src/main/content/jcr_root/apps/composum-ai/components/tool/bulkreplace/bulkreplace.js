/**
 * Class representing the Bulk Replace application.
 */
class BulkReplaceApp {
  /**
   * Constructs the BulkReplaceApp instance and initializes DOM elements.
   */
  constructor() {
    this.cacheDomElements();
    this.bindEvents();
    this.loadSavedSettings();
    this.initTooltips();
    this.currentSearchJobId = null;
    this.currentReplaceJobId = null;
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
    this.resultsZone = document.getElementById("results-zone");
    this.progressBar = document.querySelector(".progress-bar");
    this.tableBody = document.querySelector("table tbody");
    this.rootPageInput = document.getElementById("root-page");
    this.searchStringInput = document.getElementById("search-string");
    this.replacementInput = document.getElementById("replacement-string");
    this.createVersionCheckbox = document.getElementById("create-version");
    this.autoPublishCheckbox = document.getElementById("auto-publish");
  }

  /**
   * Binds event handlers to DOM elements.
   */
  bindEvents() {
    this.searchBtn.addEventListener("click", this.handleSearchClick.bind(this));
    this.replaceBtn.addEventListener("click", this.handleReplaceClick.bind(this));
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
        const page = event.target.getAttribute("data-page");
        const checked = event.target.checked;
        document.querySelectorAll(`tr[data-page="${page}"] input.select-property`).forEach(cb => cb.checked = checked);
      }
      // For individual property checkbox changes.
      if (event.target.classList.contains("select-property")) {
         // If a page checkbox is toggled via its properties, update it.
         const row = event.target.closest("tr");
         const page = row.getAttribute("data-page");
         // No additional handling needed here; state update below covers all groups.
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
      const page = pageCb.getAttribute("data-page");
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
    const saved = localStorage.getItem('aem-composumAI-bulkedit');
    if (saved) {
      try {
        const settings = JSON.parse(saved);
        if (settings.root) { this.rootPageInput.value = settings.root; }
        if (settings.search) { this.searchStringInput.value = settings.search; }
        if (settings.replacement) { this.replacementInput.value = settings.replacement; }
        if (typeof settings.createVersion !== 'undefined') { this.createVersionCheckbox.checked = settings.createVersion; }
        if (typeof settings.autoPublish !== 'undefined') { this.autoPublishCheckbox.checked = settings.autoPublish; }
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
      search: this.searchStringInput.value.trim(),
      replacement: this.replacementInput.value,
      createVersion: this.createVersionCheckbox.checked,
      autoPublish: this.autoPublishCheckbox.checked
    };
    localStorage.setItem('aem-composumAI-bulkedit', JSON.stringify(settings));
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
   * Displays a toast notification with the provided message.
   *
   * @param message the message to display
   */
  showToast(message) {
    const toastEl = document.querySelector('.toast');
    const headerTime = toastEl.querySelector('.toast-header small');
    const body = toastEl.querySelector('.toast-body');
    if (headerTime) { headerTime.textContent = "Now"; }
    if (body) { body.textContent = message; }
    // Initialize and show the toast using full jQuery Bootstrap plugin.
    $(toastEl).toast({ autohide: true, delay: 3000 });
    $(toastEl).toast('show');
  }

  /**
   * Handles the click event for the search button.
   */
  handleSearchClick() {
    this.saveSettings();
    const root = this.rootPageInput.value.trim();
    const term = this.searchStringInput.value.trim();
    if (!root || !term) {
      this.showToast("Please provide both root page and search string.");
      return;
    }
    this.tableBody.innerHTML = "";
    this.progressBar.style.width = "0%";
    this.progressBar.textContent = "0%";
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
  handleJobResponse(response) {
    if (response.status === 202) {
      return response.json();
    }
    throw new Error("Job could not be started.");
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
        this.replaceBtn.disabled = false;
        evtSource.close();
      });
      evtSource.onerror = (e) => {
        console.error("EventSource error:", e);
        evtSource.close();
      };
    }
    // Remove event stream handling for replace.
  }

  /**
   * Handles page events received via EventSource.
   *
   * @param event the event containing page data
   */
  handlePageEvent(event) {
    const data = JSON.parse(event.data);
    const headerRow = document.createElement("tr");
    headerRow.classList.add("table-secondary");
    if (data.matches) {
      // Create a page row with a checkbox for toggling all properties of this page.
      headerRow.innerHTML = `
        <td><input type="checkbox" class="select-page" data-page="${data.page}"></td>
        <td colspan="3"><strong>${data.page}</strong> (${data.matches.length} matches)</td>
      `;
      this.tableBody.appendChild(headerRow);
      for (let i = 0; i < data.matches.length; i++) {
        const match = data.matches[i];
        const propRow = document.createElement("tr");
        propRow.setAttribute("data-page", data.page);
        propRow.setAttribute("data-component", match.componentPath);
        propRow.setAttribute("data-property", match.property);
        propRow.innerHTML = `
          <td><input type="checkbox" class="select-property" checked></td>
          <td>${match.componentPath}</td>
          <td>${match.property}</td>
          <td>${match.excerpt}</td>
        `;
        this.tableBody.appendChild(propRow);
      }
    } else if (data.changed) {
      // Create a replaced row without checkboxes.
      headerRow.classList.add("replaced-row");
      headerRow.innerHTML = `
        <td colspan="4"><strong>Replaced on page:</strong> ${data.page} (${data.changed.length} properties)</td>
      `;
      this.tableBody.appendChild(headerRow);
    }
    this.updateIndeterminateStates();
  }

  /**
   * Handles the click event for the replace button, processing pages sequentially.
   */
  async handleReplaceClick() {
    this.saveSettings();
    const root = this.rootPageInput.value.trim();
    const term = this.searchStringInput.value.trim();
    const replacement = this.replacementInput.value;
    if (!root || !term || replacement === null) {
      this.showToast("Please provide root page, search string and replacement.");
      return;
    }
    const selectedCheckboxes = document.querySelectorAll("input.select-property:checked");
    if (selectedCheckboxes.length === 0) {
      this.showToast("No properties selected for replacement.");
      return;
    }
    // Group targets per page
    const pageMap = {};
    selectedCheckboxes.forEach(checkbox => {
      const row = checkbox.closest("tr");
      const page = row.getAttribute("data-page");
      const component = row.getAttribute("data-component");
      const property = row.getAttribute("data-property");
      if (!pageMap[page]) { pageMap[page] = []; }
      pageMap[page].push({ componentPath: component, property: property });
    });
    const pages = Object.keys(pageMap);
    let completed = 0;
    this.progressBar.style.width = "0%";
    this.progressBar.textContent = "0%";
    // Process each page sequentially using async/await
    for (const page of pages) {
      try {
        await this.startReplaceJobForPage(
          page,
          term,
          replacement,
          pageMap[page],
          this.createVersionCheckbox.checked,
          this.autoPublishCheckbox.checked
        );
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
  startReplaceJobForPage(page, term, replacement, targets, createVersion, autoPublish) {
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
      }).then(response => {
        if (!response.ok) {
          throw new Error("Replace operation failed for " + page);
        }
        // Directly parse and return the JSON response (no event stream).
        return response.json();
      });
    });
  }

  /**
   * Handles errors by displaying a toast notification.
   *
   * @param error the error object
   */
  handleError(error) {
    this.showToast(error.message);
  }

  /**
   * Initializes Bootstrap tooltips on the page.
   */
  initTooltips() {
    // Initialize Bootstrap tooltips for all elements with data-toggle="tooltip"
    $(function () {
      $('[data-toggle="tooltip"]').tooltip();
    });
  }
}

/**
 * Handles the DOMContentLoaded event to initialize the application.
 */
function domContentLoadedHandler() {
  BulkReplaceApp.initApp();
}

document.addEventListener("DOMContentLoaded", domContentLoadedHandler);


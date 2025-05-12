class BulkReplaceApp {
  constructor() {
    this.cacheDomElements();
    this.bindEvents();
    this.loadSavedSettings();
    this.currentSearchJobId = null;
    this.currentReplaceJobId = null;
  }

  static initApp() {
    new BulkReplaceApp();
  }

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

  getCSRFToken() {
    return fetch("/libs/granite/csrf/token.json")
      .then(response => response.json())
      .then(data => data.token);
  }

  handleSearchClick() {
    this.saveSettings();
    const root = this.rootPageInput.value.trim();
    const term = this.searchStringInput.value.trim();
    if (!root || !term) {
      alert("Please provide both root page and search string.");
      return;
    }
    this.tableBody.innerHTML = "";
    this.progressBar.style.width = "0%";
    this.progressBar.textContent = "0%";
    this.replaceBtn.disabled = true;
    this.startSearchJob(root, term);
  }

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
      .catch(this.handleError);
  }

  handleJobResponse(response) {
    if (response.status === 202) {
      return response.json();
    }
    throw new Error("Job could not be started.");
  }

  attachEventSource(operation, jobId) {
    const evtSource = new EventSource("/bin/cpm/ai/bulkreplace?operation=" + operation + "&jobId=" + jobId);
    evtSource.addEventListener("page", this.handlePageEvent.bind(this));
    if (operation === "search") {
      evtSource.addEventListener("summary", (event) => {
        this.replaceBtn.disabled = false;
        evtSource.close();
      });
    } else if (operation === "replace") {
      evtSource.addEventListener("result", (event) => {
        const result = JSON.parse(event.data);
        this.progressBar.style.width = "100%";
        this.progressBar.textContent = "100%";
        alert("Replacement completed.\nPages changed: " + result.pages +
              "\nProperties updated: " + result.properties +
              "\nSkipped: " + result.skipped);
        evtSource.close();
      });
      evtSource.addEventListener("page", (event) => {
        this.handlePageEvent(event);
      });
    }
    evtSource.onerror = (e) => {
      console.error("EventSource error:", e);
      evtSource.close();
    };
  }

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
      headerRow.innerHTML = `
        <td colspan="4"><strong>Replaced on page:</strong> ${data.page} (${data.changed.length} properties)</td>
      `;
      this.tableBody.appendChild(headerRow);
    }
    this.updateIndeterminateStates();
  }

  handleReplaceClick() {
    this.saveSettings();
    const root = this.rootPageInput.value.trim();
    const term = this.searchStringInput.value.trim();
    const replacement = this.replacementInput.value;
    if (!root || !term || replacement === null) {
      alert("Please provide root page, search string and replacement.");
      return;
    }
    const selectedCheckboxes = document.querySelectorAll("input.select-property:checked");
    if (selectedCheckboxes.length === 0) {
      alert("No properties selected for replacement.");
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
    // Process each page replacement separately
    pages.forEach(page => {
      this.startReplaceJobForPage(page, term, replacement, pageMap[page], this.createVersionCheckbox.checked, this.autoPublishCheckbox.checked)
        .then(() => {
          completed++;
          const progress = Math.round((completed / pages.length) * 100);
          this.progressBar.style.width = progress + "%";
          this.progressBar.textContent = progress + "%";
        })
        .catch(this.handleError);
    });
  }

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
        return response.json();
      });
    });
  }

  handleError(error) {
    alert(error.message);
  }
}

function domContentLoadedHandler() {
  BulkReplaceApp.initApp();
}

document.addEventListener("DOMContentLoaded", domContentLoadedHandler);

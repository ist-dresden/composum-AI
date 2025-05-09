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
    this.csrfInput = document.getElementById(":cq_csrf_token");
  }

  bindEvents() {
    this.searchBtn.addEventListener("click", this.handleSearchClick.bind(this));
    this.replaceBtn.addEventListener("click", this.handleReplaceClick.bind(this));
  }

  loadSavedSettings() {
    const saved = localStorage.getItem('aem-composumAI-bulkedit');
    if (saved) {
      try {
        const settings = JSON.parse(saved);
        if (settings.root) { this.rootPageInput.value = settings.root; }
        if (settings.search) { this.searchStringInput.value = settings.search; }
        if (settings.replacement) { this.replacementInput.value = settings.replacement; }
      } catch (e) {
        console.error("Error parsing saved settings", e);
      }
    }
  }

  saveSettings() {
    const settings = {
      root: this.rootPageInput.value.trim(),
      search: this.searchStringInput.value.trim(),
      replacement: this.replacementInput.value
    };
    localStorage.setItem('aem-composumAI-bulkedit', JSON.stringify(settings));
  }

  getCSRFToken() {
    return fetch("/libs/granite/csrf/token.json")
      .then(response => response.json())
      .then(data => {
        this.csrfInput.value = data.token;
        return data.token;
      });
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
        formData.append(":cq_csrf_token", token);
        
        return fetch("/bin/cpm/ai/bulkreplace", {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "X-CSRF-Token": token
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
      headerRow.innerHTML = `
        <td><input type="checkbox" class="select-page" data-page="${data.page}" disabled></td>
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
    const targets = [];
    for (let i = 0; i < selectedCheckboxes.length; i++) {
      const checkbox = selectedCheckboxes[i];
      const row = checkbox.closest("tr");
      const page = row.getAttribute("data-page");
      const component = row.getAttribute("data-component");
      const property = row.getAttribute("data-property");
      targets.push(page + "::" + component + "::" + property);
    }
    this.progressBar.style.width = "0%";
    this.progressBar.textContent = "0%";
    this.startReplaceJob(root, term, replacement, targets);
  }

  startReplaceJob(root, term, replacement, targets) {
    const formData = new URLSearchParams();
    formData.append("operation", "replace");
    formData.append("rootPath", root);
    formData.append("term", term);
    formData.append("replacement", replacement);
    for (let i = 0; i < targets.length; i++) {
      formData.append("target", targets[i]);
    }

    this.getCSRFToken()
      .then(token => {
        formData.append(":cq_csrf_token", token);
        
        return fetch("/bin/cpm/ai/bulkreplace", {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "X-CSRF-Token": token
          },
          body: formData.toString()
        });
      })
      .then(this.handleJobResponse.bind(this))
      .then((data) => {
         this.currentReplaceJobId = data.jobId;
         this.attachEventSource("replace", this.currentReplaceJobId);
      })
      .catch(this.handleError);
  }

  handleError(error) {
    alert(error.message);
  }
}

function domContentLoadedHandler() {
  BulkReplaceApp.initApp();
}

document.addEventListener("DOMContentLoaded", domContentLoadedHandler);

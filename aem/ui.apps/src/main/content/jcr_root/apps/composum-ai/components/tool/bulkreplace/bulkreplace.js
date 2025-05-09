class BulkReplaceApp {
  constructor() {
    this.cacheDomElements();
    this.bindEvents();
    this.currentJobId = null;
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
  }

  bindEvents() {
    this.searchBtn.addEventListener("click", this.handleSearchClick.bind(this));
    this.replaceBtn.addEventListener("click", this.handleReplaceClick.bind(this));
  }

  handleSearchClick() {
    const root = this.rootPageInput.value.trim();
    const term = this.searchStringInput.value.trim();
    if (!root || !term) {
      alert("Please provide both root page and search string.");
      return;
    }
    // Clear previous results and disable Replace until search completes
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

    fetch("/bin/cpm/ai/bulkreplace", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: formData.toString()
    })
      .then(this.handleSearchResponse.bind(this))
      .then((data) => {
         this.currentJobId = data.jobId;
         this.attachEventSource(this.currentJobId);
      })
      .catch(this.handleError);
  }

  handleSearchResponse(response) {
    if (response.status === 202) {
      return response.json();
    }
    throw new Error("Search job could not be started.");
  }

  attachEventSource(jobId) {
    const evtSource = new EventSource("/bin/cpm/ai/bulkreplace?operation=search&jobId=" + jobId);
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

  handlePageEvent(event) {
    const pageData = JSON.parse(event.data);
    // Append a header row for the page
    const headerRow = document.createElement("tr");
    headerRow.classList.add("table-secondary");
    headerRow.innerHTML = `
      <td><input type="checkbox" class="select-page" data-page="${pageData.page}" disabled></td>
      <td colspan="3"><strong>${pageData.page}</strong> (${pageData.matches.length} matches)</td>
    `;
    this.tableBody.appendChild(headerRow);
    // Append rows for each matching property
    for (let i = 0; i < pageData.matches.length; i++) {
      const match = pageData.matches[i];
      const propRow = document.createElement("tr");
      // Set data attributes for later use during replacement
      propRow.setAttribute("data-page", pageData.page);
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
  }

  handleReplaceClick() {
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
    this.startReplace(root, term, replacement, targets);
  }

  startReplace(root, term, replacement, targets) {
    const formData = new URLSearchParams();
    formData.append("operation", "replace");
    formData.append("rootPath", root);
    formData.append("term", term);
    formData.append("replacement", replacement);
    for (let i = 0; i < targets.length; i++) {
      formData.append("target", targets[i]);
    }

    fetch("/bin/cpm/ai/bulkreplace", {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: formData.toString()
    })
      .then(this.handleReplaceStream.bind(this))
      .then(() => {
        this.progressBar.style.width = "100%";
        this.progressBar.textContent = "100%";
        alert("Replacement completed.");
      })
      .catch(this.handleError);
  }

  handleReplaceStream(response) {
    if (!response.ok) {
      throw new Error("Replace operation failed.");
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let totalEvents = 0;
    return this.readStream(reader, decoder, buffer, totalEvents);
  }

  readStream(reader, decoder, buffer, totalEvents) {
    return reader.read().then(({ done, value }) => {
      if (done) return;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop();
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        if (line.indexOf("event:") === 0) {
          totalEvents++;
          const progress = Math.min(100, (totalEvents * 10));
          this.progressBar.style.width = progress + "%";
          this.progressBar.textContent = progress + "%";
        }
      }
      return this.readStream(reader, decoder, buffer, totalEvents);
    });
  }

  handleError(error) {
    alert(error.message);
  }
}

// Use a named function for DOMContentLoaded
function domContentLoadedHandler() {
  BulkReplaceApp.initApp();
}

document.addEventListener("DOMContentLoaded", domContentLoadedHandler);

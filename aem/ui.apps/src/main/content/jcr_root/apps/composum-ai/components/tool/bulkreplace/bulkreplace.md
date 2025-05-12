# Bulk‑Replace Tool (AEMaaCS)

## Purpose

Locate and replace a text string across an entire subtree of pages. Matching is **always literal & case‑insensitive**;
all actions run under the permissions of the logged‑in author.

---

## 1. Layout (single panel)

### 1.1 Header bar

* Console title **Bulk Replace**

### 1.2 Form zone

* Uses a horizontal form layout, with labels on the left and fields on the right, spanning full width.
* **Root Page** – required text field.
* **Search String** – required text field.
* **Replacement String** – text field (leave empty to delete occurrences).
* Two inline checkboxes:
    - **Create Version** – if checked, a version of the page is created before any replacement changes.
    - **Auto‑Publish** – if checked, the page is automatically published after replacement provided it qualifies as
      automatically publishable. In this context, auto‑publishing occurs only if the page’s last modification timestamp
      does not indicate changes after its last publication—ensuring consistency with the published state.
* **Action buttons**
    - **Search** – starts a search job.
    - **Replace** – iterates over still‑selected property rows and replaces each one via individual calls.

*Note: Whenever any action button is pressed, the current state of the input fields is automatically saved to
localStorage under the key `aem-composumAI-bulkedit`. Saved settings are reloaded on subsequent page loads, restoring
the form content.*

### 1.3 Results & Progress zone

* **Grouped table (initially empty)** that spans the full width of the page.
    - The table header includes a checkbox to select/deselect all property checkboxes across all pages.
    - Each page row contains a checkbox to select/deselect all property checkboxes for that page.
    - Individual property rows also have checkboxes for fine‑grained selection.
    - **Replaced rows:** Once a replacement is done for a page, the corresponding row displays a very light pastel green
      background (#d8f7d8) to indicate success, and checkboxes for that row are no longer shown.
* **Progress bar** – updated after each property replacement call.
* **Toast notification** – pops on completion, summarising successes and skips.

---

## 2. Behaviour

* Scans **all descendant pages** and examines every string property, including RTE HTML text nodes.
* **Search** is a **two‑step job**:
    1. Client POSTs parameters; server replies with `202 Accepted` and a JSON payload containing a `jobId`.
    2. Client opens an `EventSource` (GET) with that `jobId` and receives streamed results per page.
* **Replace** issues one POST per page, accepting multiple replacement targets. It is **not streaming** – each call is
  triggered individually when the replace button is pressed, and the progress bar is advanced after each call.
  Additionally, the replace operation now uses a JSON request instead of a form multipart POST.

* Auto‑publishing (triggered when **Auto‑Publish** is checked) replicates the modified page if it is deemed
  automatically publishable, meaning that its last modification does not conflict with its replication state. This
  ensures only pages that remain in a consistent, pre‑modified published state are automatically published.

---

## 3. Servlet API (`/bin/cpm/ai/bulkreplace`)

// ...existing API documentation...

*End*

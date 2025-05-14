# Bulk‑Replace Tool (AEMaaCS)

## Purpose

Locate and replace a text string across an entire subtree of pages. Matching is **always literal**;
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
    - **Create Version** – if checked, a version of the page is created before any replacement changes. (Selected by
      default).
    - **Auto‑Publish** – if checked, the page is automatically published after replacement provided it qualifies as
      automatically publishable. In this context, auto‑publishing occurs only if the page’s last modification timestamp
      does not indicate changes after its last publication—ensuring consistency with the published state.
* **Action buttons**
    - **Search** – starts a search job.
    - **Replace** – iterates over still‑selected property rows and replaces each one via individual calls.
    - **Clear Form** - clears the form. The localStorage `aem-composumAI-bulkedit-formstate` is cleared as well.
    - **Export History** - if pressed, exports the last changes (from localStorage `aem-composumAI-bulkedit-replaced`)
      into a CSV.
    - **Clear History** - clears the history of last changes.

*Note: Whenever any action button is pressed, the current state of the input fields is automatically saved to
localStorage under the key `aem-composumAI-bulkedit-formstate`. Saved settings are reloaded on subsequent page loads,
restoring the form content.*

### 1.3 Results & Progress zone

* **Error alert** – a danger‑style bootstrap alert that is displayed on errors (the error message returned in the response) and hidden on the next search/replace.
* **Grouped table (initially empty)** that spans the full width of the page.
    - The table header includes a checkbox to select/deselect all property checkboxes across all pages.
    - The shown page path is a link to the page in the editor (`/editor.html` + path + `.html`).
    - Each page row contains a checkbox to select/deselect all property checkboxes for that page.
    - The componentPath is small and a link to the editor with anchor `#scrolltocomponent-` + componentPath if it's 
      not jcr:content, if jcr:content it's a link to the page properties 
      `/mnt/overlay/wcm/core/content/sites/properties.html?item=` and the 
      path.
    - Individual property rows also have checkboxes for fine‑grained selection.
    - **Replaced rows:** Once a replacement is done for a page, the corresponding row displays a very light pastel green
      background (#d8f7d8) to indicate success, and checkboxes for that row are no longer shown. The text excerpt is
      changed to an excerpt with the replacement text.
* **Progress bar** – updated after each property replacement call.

---

## 2. Behaviour

* Scans **all descendant pages** and examines every string property, including RTE HTML text nodes. Properties
  starting with / or http:: or https:: are ignored (paths and URLs).
* **Search** is a **two‑step job**:
    1. Client POSTs parameters; server replies with `202 Accepted` and a JSON payload containing a `jobId`.
    2. Client opens an `EventSource` (GET) with that `jobId` and receives streamed results per page.
       When search is running, the progress bar is set to 50% and when it ends the progress bar is set to 100%
* **Replace** issues one POST per page, accepting multiple replacement targets. It is **not streaming** – each call is
  triggered individually when the replace button is pressed, and the progress bar is advanced after each call.
  Additionally, the replace operation now uses a JSON request instead of a form multipart POST.

* Auto‑publishing (triggered when **Auto‑Publish** is checked) replicates the modified page if it is deemed
  automatically publishable, meaning that its last modification does not conflict with its replication state. This
  ensures only pages that remain in a consistent, pre‑modified published state are automatically published.

* If any exception occurs in one of the requests, the error is transmitted in the response and shown in the error alert.

---

## 3. Servlet API (`/bin/cpm/ai/bulkreplace`)

### 3.1 Common

* **CSRF:** Granite CSRF filter is active.
    - The CSRF token is obtained dynamically before each POST and is sent in the request header `CSRF-Token`.
* **Content‑Type:** `application/x-www-form-urlencoded`.
* **operation** parameter determines the action.

### 3.2 Operation `search` (two phases)

#### 3.2.1 Start job – `POST`

| Field       | Required      | Notes                                  |
|-------------|---------------|----------------------------------------|
| `operation` | ✔︎ (`search`) |                                        |
| `rootPath`  | ✔︎            | Subtree root (e.g. `/content/site/en`) |
| `term`      | ✔︎            | Literal search text                    |

*Response*

```
HTTP/1.1 202 Accepted
Content-Type: application/json

{"jobId":"e7e4b9a8-3c2f-4a2f-8875-a8b1bce82c91"}
```

#### 3.2.2 Stream results – `GET`

```
GET /bin/cpm/ai/bulkreplace?operation=search&jobId=<uuid>
Accept: text/event-stream
```

*Response (`200 text/event-stream`)*

```
event: page
data: {"page":"/content/site/en/about",
       "matches":[
         {"componentPath":"text","property":"text","excerpt":"About Foo Company"},
         {"componentPath":"header","property":"jcr:title","excerpt":"Foo – Who we are"}
       ]}

event: summary
data: {"pages":12,"matches":42}
```

### 3.3 Operation `replace` (multiple properties per page call) – `POST`

| Field           | Required       | Notes                                                                                                                                                                               |
|-----------------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `operation`     | ✔︎ (`replace`) |                                                                                                                                                                                     |
| `page`          | ✔︎             | Absolute page path; all replacements on this page                                                                                                                                   |
| `term`          | ✔︎             | String to replace                                                                                                                                                                   |
| `replacement`   | ✔︎             | New text (empty string → deletion)                                                                                                                                                  |
| `targets`       | ✔︎             | JSON array of target objects. Each object must have:                                                                                                                                |
|                 |                | • `componentPath`: Sub‑path under `jcr:content`                                                                                                                                     |
|                 |                | • `property`: Property name (e.g. `text`)                                                                                                                                           |
| `createVersion` | Optional       | Boolean; if true, creates a version before replacement                                                                                                                              |
| `autoPublish`   | Optional       | Boolean; if true, publishes the page after replacement provided the page qualifies as automatically publishable (i.e. its last modification is not later than its last replication) |

*Note:* This operation expects a JSON request (with `Content-Type: application/json`).

*Example JSON Request:*

```json
{
  "operation": "replace",
  "page": "/content/site/en/about",
  "term": "searchTerm",
  "replacement": "newText",
  "targets": [
    {
      "componentPath": "text",
      "property": "text"
    },
    {
      "componentPath": "header",
      "property": "jcr:title"
    }
  ],
  "createVersion": true,
  "autoPublish": false
}
```

*Response*

```json
{
  "page": "/content/site/en/about",
  "time": "1747220524396",
  "changed": [
    {
      "componentPath": "text",
      "property": "text",
      "excerpt": "…context with newText…",
      "oldValue": "…context with searchTerm…",
      "newValue": "…context with newText…"
    },
    {
      "componentPath": "header",
      "property": "jcr:title",
      "excerpt": "…Foo – newText…",
      "oldValue": "…Foo – searchTerm…",
      "newValue": "…Foo – newText…"
    }
  ]
}
```

The changes are collected in localStorage in an array at key `aem-composumAI-bulkedit-replaced`

---

*End*

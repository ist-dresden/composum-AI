# Bulk‑Replace Tool (AEMaaCS)

## Purpose

Locate and replace a text string across an entire subtree of pages. Matching is **always literal & case‑insensitive**; all actions run under the permissions of the logged‑in author.

---

## 1. Layout (single panel)

### 1.1 Header bar

* Console title **Bulk Replace**.

### 1.2 Form zone

* **Root Page** – path browser (`foundation‑autocomplete`).
* **Search String** – required text field.
* **Replacement String** – text field (leave empty to delete occurrences).
* **Action buttons**
  **Search** – finds all matches.
  **Replace** – performs replacement on the rows still selected in the results table.

### 1.3 Results & Progress zone (visible after Search)

* **Grouped table**

  * **Page header row** – shows full page path and count of matches (not selectable).
  * **Property rows** – under each page header:

    | Select | Component sub‑path | Property | Text excerpt containing the search string |
    | ------ | ------------------ | -------- | ----------------------------------------- |
  * Selection drives what the **Replace** action will touch.
* **Progress bar** – appears above the table during Replace; table becomes read‑only while active.
* **Toast notification** – pops on completion, summarising successes and skips.

---

## 2. Behaviour

* Always scans **all descendant pages** and checks every string property plus RTE HTML text nodes.
* **Search** streams matches without modifying content.
* **Replace** commits changes for still‑selected property rows; pages lacking write permission are skipped with per‑row warnings.
* Progress is sent via Server‑Sent Events (SSE) and drives the progress bar and toast.

---

## 3. Servlet API (`/bin/cpm/ai/bulkreplace`)

### 3.1 Common request envelope

* **Method:** `POST`
* **Content‑Type:** `application/x-www-form-urlencoded`
* **Query parameter:** `operation=search` **or** `operation=replace`
* **CSRF:** Granite token header required

### 3.2 Operation `search`

| Form field | Type   | Required | Notes                                   |
| ---------- | ------ | -------- | --------------------------------------- |
| `rootPath` | string | ✔︎       | Subtree root (e.g. `/content/site/en`)  |
| `term`     | string | ✔︎       | Case‑insensitive literal string to find |

**Response:** `200 text/event-stream`

```
event: page
data: {"page":"/content/site/en/about",
       "matches":[
         {"componentPath":"jcr:content/text","property":"text","excerpt":"About Foo Company"},
         {"componentPath":"jcr:content/header","property":"jcr:title","excerpt":"Foo – Who we are"}
       ]}

event: summary
data: {"pages":12,"matches":42}
```

### 3.3 Operation `replace`

| Form field    | Type                 | Required | Notes                                                                                                                    |
| ------------- | -------------------- | -------- | ------------------------------------------------------------------------------------------------------------------------ |
| `rootPath`    | string               | ✔︎       | Same as above                                                                                                            |
| `term`        | string               | ✔︎       | String to replace (case‑insensitive)                                                                                     |
| `replacement` | string               | ✔︎       | New text (empty string → deletion)                                                                                       |
| `target`      | string (multi‑value) | ✔︎       | Each occurrence to modify, encoded as `page::componentPath::property`. Repeat the `target` param for every selected row. |

**Response:** `200 text/event-stream`

```
event: page
data: {"page":"/content/site/en/about",
       "changed":[
         {"componentPath":"jcr:content/text","property":"text"},
         {"componentPath":"jcr:content/header","property":"jcr:title"}
       ]}

event: result
data: {"pages":10,"properties":35,"skipped":2,"durationMs":1350}
```

### 3.4 Error codes

| Status | Meaning                                         |
| ------ | ----------------------------------------------- |
| `400`  | Missing/invalid parameters or unknown operation |
| `403`  | Author lacks read/modify ACL on `rootPath`      |
| `500`  | Unexpected server error                         |

---

*End*

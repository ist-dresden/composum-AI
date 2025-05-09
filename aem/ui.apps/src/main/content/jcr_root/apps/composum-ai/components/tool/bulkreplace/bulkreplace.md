# Bulk‑Replace Tool (AEMaaCS)

## Purpose

Locate and replace a text string across an entire subtree of pages. Matching is **always literal & case‑insensitive**; all actions run under the permissions of the logged‑in author.

---

## 1. Layout (single panel)

### 1.1 Header bar

* Console title **Bulk Replace**

### 1.2 Form zone

* Uses a horizontal form layout, with labels on the left and fields on the right, spanning full width.
* **Root Page** – required text field.
* **Search String** – required text field.
* **Replacement String** – text field (leave empty to delete occurrences).
* **Action buttons**
  - **Search** – starts a search job.
  - **Replace** – iterates over still‑selected property rows and replaces each one via individual calls.
  
*Note: Whenever any action button is pressed, the current state of the input fields is automatically saved to localStorage under the key `aem-composumAI-bulkedit`. Saved settings are reloaded on subsequent page loads, restoring the form content.*

### 1.3 Results & Progress zone

* **Grouped table (initially empty)** that spans the full width of the page.
  - Page and property rows are added dynamically based on search results.
* **Progress bar** – updated after each property replacement call.
* **Toast notification** – pops on completion, summarising successes and skips.

---

## 2. Behaviour

* Scans **all descendant pages** and examines every string property, including RTE HTML text nodes.
* **Search** is a **two‑step job**:
  1. Client POSTs parameters; server replies with `202 Accepted` and a JSON payload containing a `jobId`.
  2. Client opens an `EventSource` (GET) with that `jobId` and receives streamed results per page.
* **Replace** issues one POST per selected property row and updates the local progress bar. Pages without modify permissions are skipped with warnings.

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
| ----------- | ------------- | -------------------------------------- |
| `operation` | ✔︎ (`search`) |                                        |
| `rootPath`  | ✔︎            | Subtree root (e.g. `/content/site/en`) |
| `term`      | ✔︎            | Literal, case‑insensitive search text  |

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

### 3.3 Operation `replace` (single property per call) – `POST`

| Field           | Required       | Notes                                                 |
| --------------- | -------------- | ----------------------------------------------------- |
| `operation`     | ✔︎ (`replace`) |                                                       |
| `page`          | ✔︎             | Absolute page path                                   |
| `componentPath` | ✔︎             | Sub‑path under page content relative to `jcr:content` |
| `property`      | ✔︎             | Property name (e.g. `text`)                           |
| `term`          | ✔︎             | String to replace (case‑insensitive)                 |
| `replacement`   | ✔︎             | New text (empty string → deletion)                   |

*Response*

```
HTTP/1.1 200 OK
Content-Type: application/json

{"replaced":true}
```

If the author lacks modify ACL on that property, server returns `403`.

### 3.4 Error codes

| Status | Meaning                                         |
| ------ | ----------------------------------------------- |
| `400`  | Missing/invalid parameters or unknown operation |
| `403`  | Author lacks read/modify ACL                    |
| `404`  | `jobId` not found (during GET search)           |
| `500`  | Unexpected server error                         |

---

*End*

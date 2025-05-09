# Bulk‑Replace Tool (AEMaaCS)

## Purpose

Find and replace a text string across an entire subtree of pages. All operations run with the permissions of the currently logged‑in author.

---

## Layout

### Header bar

* Console title **Bulk Replace**

### Form zone

* **Root Page** – *foundation‑autocomplete* path browser (single selection).
* **Search String** – required Coral.Textfield.
  Toggles under the field: **Regex?**, **Case‑sensitive?**
* **Replacement String** – Coral.Textfield (leave empty to delete occurrences).
* **Action buttons**
  **Search** – locates all matches.
  **Replace** – executes replacement on the rows still selected in the results table.

### Results & Progress zone (appears after Search)

* **Grouped table**

  * A **page header row** shows the full page path and a count of matches on that page. This header is not selectable.
  * **Property rows** under each header contain:

    | Select | Component sub‑path | Property | Text excerpt containing the search string |
    | ------ | ------------------ | -------- | ----------------------------------------- |
  * Authors select/deselect property rows to refine the replacement scope. All property rows under an unselected page header remain unaffected.
* **Summary chip** – displays “*n* pages – *k* matches”.
* **Progress bar** – appears above the table during **Replace**; determinate based on processed/total matches. The table becomes read‑only while the bar is active.
* **Toast notification** – pops on completion, summarising successes and skips.

---

## Behaviour

* Always scans **all descendant pages**; no scope toggles are shown.
* Includes every string property and RTE HTML node.
* **Search** gathers and displays matches—no content is changed.
* **Replace** commits changes for the rows still selected after review; writes occur in batches under the author’s session. Pages without write permission generate row‑level warnings and are skipped.
* Progress events stream via Server‑Sent Events to update the bar and toast.

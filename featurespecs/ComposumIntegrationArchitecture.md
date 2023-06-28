# General Architecture of the Composum Integration

See also [Composum Integration.md](../composum/ComposumIntegration.md) for specific details.

## Naming

Java classes are at com.composum.ai.bundle or in subpackages like com.composum.ai.bundle.model for models.
Resources are at path /libs/composum/pages/options/ai/ .

## Servlets

General URL prefix is /bin/cpm/platform/ai/ .

### `com.composum.ai.composum.bundle.AIDialogServlet`

at `/bin/cpm/platform/ai/dialog` : serves the HTML for the dialogs. Follows the usual Composum
AbstractServiceServlet pattern with an URL like
`/bin/cpm/platform/ai/dialog.{operation}.{extension}/{resourcesuffix}?{optional parameters}`
, for example
`/bin/cpm/platform/ai/dialog.translationDialog.html/content/ist/software/home/test/_jcr_content/jcr:description?propertypath=jcr:description&pages.locale=de`
that shows the translation dialog for the property resource that is given as the suffix.
Extension can be html or json.

## `com.composum.ai.composum.bundle.AIServlet`

at `/bin/cpm/platform/ai/authoring` is a servlet providing an adapter to the the backend services that call
ChatGPT. Javascript actions will call that servlet except if they want to render a resource (in that case
ChatGPTDialogServlet is appropriate).
The URL also follows the Composum AbstractServiceServlet pattern, e.g.
http://localhost:9090/bin/cpm/platform/ai/authoring.translate.json
with operation translate and extension json, the parameters being transmitted via POST.

## Common Implementation steps

That's broken down to be partially executable by ChatGPT.

### Identifiers / filenames

Make a list specifying

- {feature} = short name of feature, e.g. create for creation dialog.
- {resourcetype} = component resource type composum/pages/options/ai/dialogs/{feature}
- {dialogURL} = `/bin/cpm/platform/ai/dialog.{feature}Dialog.html`
- ID for dialog chatgpt-{feature}-dialog
- a HTML class for all dialog fields that need to be addressed from Javascript: inputs or divs where output will be written. These don't need any common prefix as reading them out will be done using the dialog ID - just use a name appropriate to their function.
- parameter names for the inputs, when they are transmitted to the server. That will be short, must one word.

### 1. adapt ChatGPTDialogServlet

Modify the server-side servlet `com.composum.ai.composum.bundle.AIDialogServlet`: extend the servlet with a new
operation `{feature}Dialog` to serve that operation with with the {resourcetype} - it'll be available at {dialogURL}.

### 2. JSP Files

- **Feature JSP:** Create a JSP file for the new feature
  at `/libs/composum/pages/options/ai/dialogs/{feature}/{feature}.jsp`. This file will be used to render the
  dialog of the feature. This resource is derived from `com.composum.ai.composum.bundle.AIDialogServlet` and uses a
  model specific to the feature, `com.composum.ai.bundle.model.ChatGPT{Feature}DialogModel`.

### 3. CSS
/libs/composum/pages/options/ai/css/dialogs.scss 

### 4. Java Model

- **Java model**: create model specific to the feature, `com.composum.ai.bundle.model.ChatGPT{Feature}
  DialogModel`, that implements properties needed for the JSP.

### 5. JavaScript File

Modify the JavaScript file at `/libs/composum/pages/options/ai/js/chatgpt.js`.

- Add the URL constant for the {dialogUrl}
- Add a new "open{Feature}Dialog" method that loads the dialog at {dialogURL} with Ajax, modeled after the other open
  {Feature}Dialog methods
- Add a new JavaScript class `{Feature}Dialog` that initializes attributes for the inputs and outputs from the dialog,
  using the HTML classes declared in the identifier list.
- Add methods as listeners for the buttons, similarily to the other classes.
- Add a method save that writes back the result to the widget the dialog was called on.
- Add a method for the main action button (e.g. translate, create) that collects the inputs, makes an ajax call to 
  the ChatGPTServlet and writes the result to the output field (div)

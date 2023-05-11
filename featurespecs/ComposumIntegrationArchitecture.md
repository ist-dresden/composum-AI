# General Architecture of the Composum Integration

See also [Composum Integration.md](../composum/ComposumIntegration.md) for specific details.

## Naming

Java classes are at com.composum.chatgpt.bundle or in subpackages like com.composum.chatgpt.bundle.model for models.
Resources are at path /libs/composum/chatgpt/pagesintegration/ .

## Servlets

General URL prefix is /bin/cpm/platform/chatgpt/ .

### `com.composum.chatgpt.bundle.ChatGPTDialogServlet`

at `/bin/cpm/platform/chatgpt/dialog` : serves the HTML for the dialogs. Follows the usual Composum
AbstractServiceServlet pattern with an URL like
`/bin/cpm/platform/chatgpt/dialog.{operation}.{extension}/{resourcesuffix}?{optional parameters}`
, for example
`/bin/cpm/platform/chatgpt/dialog.translationDialog.html/content/ist/software/home/test/_jcr_content/jcr:description?propertypath=jcr:description&pages.locale=de`
that shows the translation dialog for the property resource that is given as the suffix.
Extension can be html or json.

## `com.composum.chatgpt.bundle.ChatGPTServlet`

at `/bin/cpm/platform/chatgpt/authoring` is a servlet providing an adapter to the the backend services that call
ChatGPT. Javascript actions will call that servlet except if they want to render a resource (in that case
ChatGPTDialogServlet is appropriate).
The URL also follows the Composum AbstractServiceServlet pattern, e.g.
http://localhost:9090/bin/cpm/platform/chatgpt/authoring.translate.json
with operation translate and extension json, the parameters being transmitted via POST.

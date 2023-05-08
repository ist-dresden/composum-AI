# Details about the Composum integration of the module

As discussed in [Project Structure](./ProjectStructure.md), the composum specific code is contained in module composum.

State: in execution; this just preliminary.

## Location of integration

Over a widget we have at the left a title and at the right a description (hint). We will add buttons at the right of 
the hint.

### Example: page description

/libs/composum/pages/stage/edit/default/page/dialog/general.jsp

<cpp:widget label="Description" property="jcr:description" type="richtext" height="150" i18n="true"
            hint="a short abstract / teaser text of the page"/>
<!-- START RENDERINFO: /libs/composum/pages/commons/widget/richtext @ sling-post : /content/ist/composum/home/jcr:content -->
label : /libs/composum/pages/commons/widget/label.jsp
<i class="fa fa-language"></i><i class="fa fa-magic"></i><i class="fa fa-tags"></i>
### Buttons for text field / text area / category filling

#### text field and text area
translation and content creation always(?) available

#### category
only for pages dialog category!

### Widget extension:
label.jsp gets sling:call to labelextension.jsp .
labelextension calls a service (PagesExtensionService) that has PagesExtensions registered, and that would return 
the JSP path to call.

PagesExtensionService -> AbstractModel (pages) .

## Possible Icons

Translation: https://fontawesome.com/v4/icon/language fa-language 

Tagging: https://fontawesome.com/v4/icon/tags fa-tags

Content creation: perhaps https://fontawesome.com/v4/icon/align-left fa-align-left
perhaps https://fontawesome.com/v4/icon/plus-square fa-plus-square
https://icons.getbootstrap.com/icons/pencil-square/ <i class="bi bi-pencil-square"></i>
perhaps https://icons.getbootstrap.com/icons/chat-dots-fill/
! https://icons.getbootstrap.com/icons/magic/ <i class="bi bi-magic"></i>
! https://fontawesome.com/v4/icon/magic <i class="fa fa-magic" aria-hidden="true"></i>

## CSS

Location widgets definitions in Pages:
pages/commons/package/src/main/content/jcr_root/libs/composum/pages/commons/css/widgets.scss
in category:composum.components.widgets[css:/libs/composum/nodes/commons/components/clientlibs/components]

## Javascript
category:composum.components.widgets[js:/libs/composum/nodes/commons/components/clientlibs/components]
relevant: pages/stage/package/src/main/content/jcr_root/libs/composum/pages/stage/edit/js/dialogs.js declares many 
dialogs. We need integration into create dialog, too - at least for content creation; translation doesn't matter 
(would be inactive, anyway). Base class: ElementDialog, covers everything. Binding of actions seems usually done in 
initView .

# To review with Ralf

- handling of variables / mixins?
- Review pages integration on pages side


PropertyHandle: ?? in chatgpt propertyName="title" , propertyPath="i18n/es/title"? What about english?

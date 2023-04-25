# Details about the Composum integration of the module

As discussed in [Project Structure](./ProjectStructure.md), the composum specific code is contained in module composum.

State: in execution; this just preliminary.

## Location of integration

Over a widget we have at the left a title and at the right a description. The integration could be icons at the 
right side of the description.

### Example: page description

/libs/composum/pages/stage/edit/default/page/dialog/general.jsp

<cpp:widget label="Description" property="jcr:description" type="richtext" height="150" i18n="true"
            hint="a short abstract / teaser text of the page"/>
<!-- START RENDERINFO: /libs/composum/pages/commons/widget/richtext @ sling-post : /content/ist/composum/home/jcr:content -->
label : /libs/composum/pages/commons/widget/label.jsp
<i class="fa fa-language"></i><i class="fa fa-magic"></i><i class="fa fa-tags"></i>


## Possible Icons

Translation: https://fontawesome.com/v4/icon/language fa-language 

Tagging: https://fontawesome.com/v4/icon/tags fa-tags

Content creation: perhaps https://fontawesome.com/v4/icon/align-left fa-align-left
perhaps https://fontawesome.com/v4/icon/plus-square fa-plus-square
https://icons.getbootstrap.com/icons/pencil-square/ <i class="bi bi-pencil-square"></i>
perhaps https://icons.getbootstrap.com/icons/chat-dots-fill/
! https://icons.getbootstrap.com/icons/magic/ <i class="bi bi-magic"></i>
! https://fontawesome.com/v4/icon/magic <i class="fa fa-magic" aria-hidden="true"></i>

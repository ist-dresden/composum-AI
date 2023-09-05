/**
 * Registration of the custom dialogs.
 * Thanks to https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713 for hints!
 */

import {ContentCreationDialog} from './ContentCreationDialog.js';

(function ($, channel, window, undefined) {
    "use strict";

    const ACTION_ICON = "coral-Icon--gearsEdit";
    const ACTION_TITLE = "Coomposum AI Content Generation";
    const ACTION_NAME = "ComposumAI";
    const CREATE_DIALOG_URL = "/mnt/override/apps/composum-ai/components/contentcreation/_cq_dialog.html/conf/composum-ai/settings/dialogs/contentcreation";

    const composumAiAction = new Granite.author.ui.ToolbarAction({
        name: ACTION_NAME,
        icon: ACTION_ICON,
        text: ACTION_TITLE,
        order: "last",
        execute: function (editable) {
            console.log("aitoolbar execute", arguments);
            showDialog(editable);
        },
        condition: function (editable) {
            console.log("aitoolbar editable", arguments);
            // TODO implement some condition where the dialog makes sence. On editable.designDialog?
            return true;
        },
        isNonMulti: true,
    });

    channel.on("cq-layer-activated", function (event) {
        if (event.layer === "Edit") {
            Granite.author.EditorFrame.editableToolbar.registerAction("Composum-AI", composumAiAction);
            console.log("Composum AI action is registered");
        }
    });

    function showDialog(editable) {
        const dialogId = 'composumAI-dialog'; // possibly use editable.path to make it unique

        $.ajax({
            url: CREATE_DIALOG_URL,
            type: "GET",
            dataType: "html",
            success: function (data) {
                // reload the dialog since otherwise we get an internal error in Coral on second show.
                $(dialogId).remove();
                // throw away HTML head and so forth:
                const dialog = $('<div>').append($.parseHTML(data)).find('coral-dialog');
                dialog.attr('id', dialogId);
                dialog.appendTo('body');
                dialog.get()[0].show(); // call Coral function on the element.
                new ContentCreationDialog(editable, dialog, retrieveContent(editable));
            }.bind(this),
            error: function (xhr, status, error) {
                console.log("error loading create dialog", xhr, status, error);
            }
        });
    }

    function retrieveContent(editable) {
        if (editable.dom.hasClass('text')) {
            return editable.dom.find('.cmp-text').text();
        } else {
            console.error('dont know how to retrieve text from editable', editable);
            debugger;
        }
    }

    function insertCreateButtons(event) {
        console.log("insertCreateButton", arguments);
        // find all coral-icon
        // <coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS" icon="infoCircle" ... </coral-icon>
        // and add another icon beside it
        // To make sure we don't do that twice we add a data-comp-ai-iconsadded attribute to the infoCircle.
        $(event.target).find('coral-icon[icon="infoCircle"][data-comp-ai-iconsadded!="true"]').each(
            function (index, element) {
                console.log("insertCreateButton element", element);
                const gearsEdit = $(
                    '<coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS composum-ai-create-dialog-action" icon="gearsEdit" role="img" size="S">\n' +
                    '  <svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">\n' +
                    '    <use xlink:href="#spectrum-icon-18-GearsEdit"></use>\n' +
                    '  </svg>\n' +
                    '</coral-icon>');
                gearsEdit.insertBefore(element);
                element.setAttribute('data-comp-ai-iconsadded', 'true');
            }
        );
    }

    channel.on('coral-overlay:open', insertCreateButtons.bind(this));

})(jQuery, jQuery(document), this);

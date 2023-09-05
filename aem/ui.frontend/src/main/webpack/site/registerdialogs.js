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

    /*
    const composumAiAction = new Granite.author.ui.ToolbarAction({
        name: ACTION_NAME,
        icon: ACTION_ICON,
        text: ACTION_TITLE,
        order: "last",
        execute: function (editable) {
            console.log("aitoolbar execute", arguments);
            showCreateDialog(editable.path, retrieveContent(editable));
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
    */

    function showCreateDialog(path, content, writebackCallback) {
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
                new ContentCreationDialog(dialog, path, content, writebackCallback);
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
        if ($(event.target).find('.composum-ai-dialog').size() > 0) {
            return; // don't insert buttons into our own dialog
        }
        $(event.target).find('div.coral-Form-fieldwrapper textarea.coral-Form-field[data-comp-ai-iconsadded!="true"]').each(
            function (index, textarea) {
                console.log("insertCreateButton textarea", textarea);
                const gearsEdit = $(
                    '<coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS composum-ai-create-dialog-action" icon="gearsEdit" role="img" size="S">\n' +
                    '  <svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">\n' +
                    '    <use xlink:href="#spectrum-icon-18-GearsEdit"></use>\n' +
                    '  </svg>\n' +
                    '</coral-icon>');
                if ($(textarea.parentElement).find('coral-icon').size() > 0) {
                    gearsEdit.addClass('composum-ai-iconshiftleft');
                }
                gearsEdit.insertAfter(textarea);
                textarea.setAttribute('data-comp-ai-iconsadded', 'true');
                gearsEdit.click(function (event) {
                    console.log("createButton click", arguments);
                    const formAction = $(textarea).closest('form').attr('action');
                    showCreateDialog(formAction, textarea.value, (newvalue) => $(textarea).val(newvalue));
                });
            }
        );
    }

    channel.on('coral-overlay:open', insertCreateButtons.bind(this));

    channel.on("foundation-contentloaded", function (e) {
        Coral.commons.ready(channel, function (component) {
            insertCreateButtons(e);
        });
    });

})
(jQuery, jQuery(document), this);

/**
 * Registration of the custom dialogs.
 * Thanks to https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713 for hints!
 */

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
            showDialog(editable);
        },
        condition: function (editable) {
            // TODO implement some condition where the dialog makes sence. On editable.designDialog?
            console.log("editable", editable);
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
                $('#composumAI-dialog').remove();
                // throw away HTML head and so forth:
                const dialog = $('<div>').append($.parseHTML(data)).find('coral-dialog');
                dialog.attr('id', dialogId);
                dialog.appendTo('body');
                dialog.get()[0].show(); // call Coral function on the element.
            }.bind(this),
            error: function (xhr, status, error) {
                console.log("error loading create dialog", xhr, status, error);
            }
        });
    }
})(jQuery, jQuery(document), this);

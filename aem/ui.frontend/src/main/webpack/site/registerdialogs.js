/**
 * Registration of the custom dialogs.
 * Thanks to https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713 for hints!
 */

(function ($, channel, window, undefined) {
    "use strict";

    var ACTION_ICON = "coral-Icon--gearsEdit";
    var ACTION_TITLE = "Coomposum AI Content Generation";
    var ACTION_NAME = "ComposumAI";

    var composumAiAction = new Granite.author.ui.ToolbarAction({
        name: ACTION_NAME,
        icon: ACTION_ICON,
        text: ACTION_TITLE,
        execute: function (editable) {
            showDialog();
        },
        condition: function (editable) {
            console.log("editable.type check", editable.type);
            console.log("editable", editable, JSON.toString(editable));
            return true; // editable && editable.type === "chatgptintegration/components/title";
        },
        isNonMulti: true,
    });

    channel.on("cq-layer-activated", function (event) {
        if (event.layer === "Edit") {
            Granite.author.EditorFrame.editableToolbar.registerAction("Composum-AI", composumAiAction);
            console.log("Composum AI action is registered");
        }
    });


    function showDialog() {
        // Create the dialog
        var dialog = new Coral.Dialog().set({
            id: 'composumAI-dialog',
            header: {
                innerHTML: 'Generate Content through Composum AI'
            },
            content: {
                innerHTML: '<form class="coral-Form coral-Form--vertical"><section class="coral-Form-fieldset"><div class="coral-Form-fieldwrapper"><textarea is="coral-textarea" class="coral-Form-field" placeholder="Enter your prompt" id="textarea1"  name="name"></textarea></div><div class="coral-Form-fieldwrapper"> <textarea is="coral-textarea" class="coral-Form-field" placeholder="Result will be displayed here" id="textarea2"  name="name"></textarea></div></section></form>'
            },
            footer: {
                innerHTML: '<button is="coral-button" variant="primary">Generate</button><button is="coral-button" variant="primary" coral-close>Close</button>'
            }
        });

        // Add an event listener to the submit button
        dialog.footer.querySelector("button").addEventListener("click", function () {
            var textarea1Value = dialog.content.querySelector("#textarea1").value;
            var servletUrl = "/bin/chat?prompt=" + encodeURIComponent(textarea1Value);

            dialog.content.querySelector("#textarea2").value = 'Generating...';

            // Send a request to the servlet
            var xhr = new XMLHttpRequest();
            xhr.open("GET", servletUrl);
            xhr.onreadystatechange = function () {
                if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
                    dialog.content.querySelector("#textarea2").value = xhr.responseText;
                }
            };
            xhr.send();
        });

        // Open the dialog
        document.body.appendChild(dialog);
        dialog.show();
    }
})(jQuery, jQuery(document), this);

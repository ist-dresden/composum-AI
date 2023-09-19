/**
 * Registration of the custom dialogs.
 * Thanks to https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713 for hints!
 */

import {ContentCreationDialog} from './ContentCreationDialog.js';
import {SidePanelDialog} from './SidePanelDialog.js';

(function ($, channel, window, undefined) {
    "use strict";

    const CREATE_DIALOG_URL =
        "/mnt/override/apps/composum-ai/components/contentcreation/_cq_dialog.html/conf/composum-ai/settings/dialogs/contentcreation";
    const SIDEPANEL_DIALOG_URL =
        "/mnt/override/apps/composum-ai/components/sidepanel-ai/_cq_dialog.html/conf/composum-ai/settings/dialogs/sidepanel-ai";

    /*
    const composumAiAction = new Granite.author.ui.ToolbarAction({
        name: "ComposumAI",
        icon: "coral-Icon--gearsEdit",
        text: "Coomposum AI Content Generation",
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
                    '<coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS composum-ai-create-dialog-action" title="AI Content Creation" icon="gearsEdit" role="img" size="S">\n' +
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
                    const formPath = $(textarea).closest('form').attr('action');
                    showCreateDialog(formPath, textarea.value, (newvalue) => $(textarea).val(newvalue));
                });
            }
        );
        registerContentDialogInRichtextEditor(event);
    }

    channel.on('coral-overlay:open', insertCreateButtons.bind(this));

    channel.on("foundation-contentloaded", function (e) {
        Coral.commons.ready(channel, function (component) {
            insertCreateButtons(e);
            loadSidebarPanelDialog();
            initRteHooks();
        });
    });

    function loadSidebarPanelDialog() {
        const dialogId = 'composumAI-sidebar-panel';
        if ($('#' + dialogId).size() > 0 || $('#SidePanel coral-tabview').size() === 0) {
            return;
        }
        $.ajax({
            url: SIDEPANEL_DIALOG_URL,
            type: "GET",
            dataType: "html",
            success: function (data) {
                if ($('#' + dialogId).size() > 0 || $('#SidePanel coral-tabview').size() === 0) {
                    return; // double check because of possible race conditions
                }

                // throw away HTML head and so forth:
                const dialog = $('<div>').append($.parseHTML(data)).find('coral-dialog');
                console.log("the dialog", dialog);
                // the first tab and panel are the actual dialog content:
                const tab = dialog.find('coral-tabview coral-tab').first();
                const panel = dialog.find('coral-tabview coral-panel').first();
                panel.attr('id', dialogId);
                // now find tablist and panelstack in the #SidePanel:
                const tabView = $('#SidePanel coral-tabview')[0];
                tabView.tabList.items.add(tab[0]);
                tabView.panelStack.items.add(panel[0]);
                new SidePanelDialog(panel);
            }.bind(this),
            error: function (xhr, status, error) {
                console.log("error loading create dialog", xhr, status, error);
            }
        });
    }

    function initRteHooks() {
        console.log('initRteHooks');
        Granite.author.ContentFrame.getDocument().on('editing-start', registerContentDialogInRichtextEditor);
    }

    function registerContentDialogInRichtextEditor(event) {
        console.log("registerContentDialogInRichtextEditor", arguments);
        const button = '<button is="coral-button" variant="quietaction" class="rte-toolbar-item _coral-ActionButton composum-ai-create-dialog-action" type="button"\n' +
            '        title="AI Content Creation" icon="gearsEdit" size="S">\n' +
            '    <coral-icon size="S"\n' +
            '                class="_coral-Icon--sizeS _coral-Icon" role="img" icon="gearsEdit" alt="AI Content Creation"\n' +
            '                aria-label="AI Content Creation">\n' +
            '        <svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">\n' +
            '            <use xlink:href="#spectrum-icon-18-GearsEdit"></use>\n' +
            '        </svg>\n' +
            '    </coral-icon>\n' +
            '    <coral-button-label class="_coral-ActionButton-label"></coral-button-label>\n' +
            '</button>\n';
        const buttongroups = $(document).find(".rte-ui > div > coral-buttongroup");
       // loop over each buttongroup and add the button if it's not there yet:
        buttongroups.each(function (index, buttongroup) {
            if ($(buttongroup).find('.composum-ai-create-dialog-action').size() === 0) {
                const $button = $(button);
                const target = event.target;
                var path = undefined;
                for (var i = 0; i < Granite.author.editables.length; i++) {
                    var editable = Granite.author.editables[i];
                    if (editable.dom[0] === target) {
                        path = editable.path;
                        break;
                    }
                }
                console.log("registerContentDialogInRichtextEditor path", path);
                $(buttongroup).append($button);
                $button.click(function (event) {
                    console.log("createButtonText click", arguments);
                    showCreateDialog(path, target.innerHtml, (newvalue) => $(textarea).val(newvalue));
                });
            }
        });
    }

})
(jQuery, jQuery(document), this);

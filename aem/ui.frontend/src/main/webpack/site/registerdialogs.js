/**
 * Registration of the custom dialogs.
 * Thanks to https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713 for hints!
 */

import {ContentCreationDialog} from './ContentCreationDialog.js';
import {SidePanelDialog} from './SidePanelDialog.js';
import {AIConfig} from './AIConfig.js';

/**
 * Main function to handle the registration of custom dialogs and their interactions.
 * @param {Object} $ - The jQuery object.
 * @param {Object} channel - The event channel for Coral UI events.
 * @param {Object} window - The global window object.
 * @param {undefined} undefined - Ensures that undefined is truly undefined.
 */
(function ($, channel, window, undefined) {
    "use strict";

    const CREATE_DIALOG_URL =
        "/mnt/override/apps/composum-ai/components/contentcreation/_cq_dialog.html/conf/composum-ai/settings/dialogs/contentcreation";
    const SIDEPANEL_DIALOG_URL =
        "/mnt/override/apps/composum-ai/components/sidepanel-ai/_cq_dialog.html/conf/composum-ai/settings/dialogs/sidepanel-ai";
    const aiconfig = new AIConfig();

    channel.on('cq-sidepanel-loaded', (event) => Coral.commons.ready(event.target, loadSidebarPanelDialog));

    /**
     * Loads the Sidebar Panel AI if it wasn't loaded already and if there's a sidebar present.
     */
    function loadSidebarPanelDialog() {
        try {
            aiconfig.ifEnabled('sidepanel', () => {
                const dialogId = 'composumAI-sidebar-panel';
                if ($('#' + dialogId).length > 0 || $('#SidePanel coral-tabview').length === 0) {
                    return;
                }
                $.ajax({
                    url: SIDEPANEL_DIALOG_URL,
                    type: "GET",
                    dataType: "html",
                    success: function (data) {
                        if ($('#' + dialogId).length > 0 || $('#SidePanel coral-tabview').length === 0) {
                            return; // double check because of possible race conditions
                        }

                        // throw away HTML head and so forth:
                        const dialog = $('<div>').append($.parseHTML(data)).find('coral-dialog');
                        console.log("found dialog", dialog);
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
            });
        } catch (e) {
            console.error("error loading sidebar panel dialog", e);
            debugger;
        }
    }

    /**
     * Opens a content creation dialog for the given field.
     * @param parameters see ContentCreationDialog constructor
     */

    function showCreateDialog(parameters) {
        const dialogId = 'composumAI-create-dialog'; // possibly use editable.path to make it unique

        $.ajax({
            url: CREATE_DIALOG_URL + "?richtext=" + parameters.isRichtext,
            type: "GET",
            dataType: "html",
            success: function (data) {
                // reload the dialog since otherwise we get an internal error in Coral on second show.
                $(dialogId).remove();
                // throw away HTML head and so forth:
                const dialog = $('<div>').append($.parseHTML(data)).find('coral-dialog');
                dialog.attr('id', dialogId);
                dialog.appendTo('body');
                $(dialog).trigger('foundation-contentloaded');
                dialog.get()[0].show(); // call Coral function on the element.
                parameters.dialog = dialog;
                new ContentCreationDialog(parameters);
            }.bind(this),
            error: function (xhr, status, error) {
                console.log("error loading create dialog", xhr, status, error);
            }
        });
    }

    const fieldlabeliconHTML =
        '<coral-icon class="coral-Form-fieldinfo _coral-Icon _coral-Icon--sizeS composum-ai-create-dialog-action" title="AI Content Creation" icon="gearsEdit" role="img" size="S">\n' +
        '  <svg focusable="false" aria-hidden="true" class="_coral-Icon--svg _coral-Icon">\n' +
        '    <use xlink:href="#spectrum-icon-18-GearsEdit"></use>\n' +
        '  </svg>\n' +
        '</coral-icon>';

    /**
     * Inserts the AI content creation buttons for text areas in the provided element.
     * @param {HTMLElement} element - The DOM element to search for textareas.
     */
    function insertCreateButtonsForTextareas(element) {
        console.log("insertCreateButton", arguments);
        if ($(element).find('.composum-ai-dialog').length > 0) {
            return; // don't insert buttons into our own dialog
        }
        $(element).find('div.coral-Form-fieldwrapper textarea.coral-Form-field[data-comp-ai-iconsadded!="true"]').each(
            function (index, textarea) {
                console.log("insertCreateButton textarea", textarea);
                const gearsEdit = $(fieldlabeliconHTML);
                if ($(textarea.parentElement).find('coral-icon').length > 0) {
                    gearsEdit.addClass('composum-ai-iconshiftleft');
                }
                gearsEdit.insertAfter(textarea);
                textarea.setAttribute('data-comp-ai-iconsadded', 'true');
                gearsEdit.click(function (event) {
                    console.log("createButton click", arguments);
                    const formPath = $(textarea).closest('form').attr('action');
                    var property = $(textarea).attr('name');
                    property = property && property.startsWith('./') && property.substring(2);
                    if (formPath && formPath.startsWith('/content')) {
                        showCreateDialog({
                            componentPath: formPath,
                            property,
                            oldContent: textarea.value,
                            writebackCallback: (newvalue) => $(textarea).val(newvalue),
                            isRichtext: false,
                            stackeddialog: true
                        });
                    } else {
                        console.error('Could not determine path of form for ', textarea && textarea.get());
                    }
                });
            }
        );
    }

    // for dialogs coral-overlay:open, for content-fragments foundation-contentloaded
    channel.on('coral-overlay:open foundation-contentloaded', prepareDialog);

    /**
     * Prepares the dialog by inserting AI content creation buttons and registering the content dialog in richtext editors.
     * @param {Event} event - The event triggering the preparation.
     */
    function prepareDialog(event) {
        console.log("prepareDialog", event.type, event.target);
        try {
            aiconfig.ifEnabled('create', () => {
                Coral.commons.ready(event.target, function () {
                    insertCreateButtonsForTextareas(event.target);
                    registerContentDialogInRichtextEditors(event);
                });
            });
        } catch (e) {
            console.error("error preparing dialog", event, e);
            debugger;
        }
    }

    channel.on('cq-layer-activated', initRteHooks);


    /**
     * Initializes hooks for the richtext editor when activated.
     * @param {Event} event - The event indicating the activation of the RTE.
     */
    function initRteHooks(event) {
        console.log("waitForReadyAndInsert", event.type, event.target);
        try {
            aiconfig.ifEnabled('create', () => {
                Coral.commons.ready(event.target, function () {
                    Granite.author.ContentFrame.getDocument()
                        .off('editing-start', registerContentDialogInRichtextEditors)
                        .on('editing-start', registerContentDialogInRichtextEditors);
                });
            });
        } catch (e) {
            console.error("error initializing RTE hooks", event, e);
            debugger;
        }
    }

    const rtebuttonHTML = '<button is="coral-button" variant="quietaction" class="rte-toolbar-item _coral-ActionButton composum-ai-create-dialog-action" type="button"\n' +
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

    var lastEditorStartTarget = undefined;

    /** Registers the content creation dialog in the richtext editor toolbar.
     * The event can either be an opening event for a dialog, or an editing-start for an RTE in the document. */
    /**
     * Registers the content creation dialog in the richtext editor toolbar.
     * @param {Event} registerevent - The event triggering the registration.
     */
    function registerContentDialogInRichtextEditors(registerevent) {
        console.log("registerContentDialogInRichtextEditors", registerevent.type, registerevent.target);
        if (registerevent.type === 'editing-start') {
            lastEditorStartTarget = registerevent.target;
        }

        const buttongroups = $(document).find(".rte-ui > div > coral-buttongroup");
        // loop over each buttongroup and add the button if it's not there yet:
        buttongroups.each(function (index, buttongroup) {
            if ($(buttongroup).closest('.composum-ai-dialog').length > 0) {
                return; // don't insert buttons into our own dialogs
            }
            if ($(buttongroup).find('.composum-ai-create-dialog-action').length === 0) {
                // console.log("registerContentDialogInRichtextEditors path", path);
                const formaction = $(buttongroup).closest('form[action]').attr('action'); // if rte in dialog
                var path = undefined;
                if (formaction && formaction.startsWith('/content')) {
                    path = formaction;
                }
                path = path || $(buttongroup).closest('[data-path]').attr('data-path');
                var property = $(buttongroup).closest('.richtext-container').find('[data-cq-richtext-editable=true]').attr('name');
                property = property && property.startsWith('./') && property.substring(2);
                const $button = $(rtebuttonHTML);
                $(buttongroup).append($button);
                $button.click(function (clickevent) {
                    console.log("createButtonText click", typeof clickevent.type, clickevent.target);
                    var componentPath = path;
                    // in case of rte in content it's difficult to find the path from the button or current state,
                    // so we determine it from the last editor start event:
                    if (!componentPath && lastEditorStartTarget) {
                        for (var i = 0; i < Granite.author.editables.length; i++) {
                            var editable = Granite.author.editables[i];
                            if (editable.dom[0] === lastEditorStartTarget) {
                                componentPath = editable.path;
                                break;
                            }
                        }
                    }
                    if (!componentPath) { // FIXME
                        debugger;
                    }

                    var rteinstance = $(buttongroup).closest('.cq-RichText').find('.cq-RichText-editable').data('rteinstance');
                    rteinstance = rteinstance || $(lastEditorStartTarget).data('rteinstance');
                    rteinstance = rteinstance || $(buttongroup).closest('[data-form-view-container=true]').find('[data-cfm-richtext-editable=true]').data('rteinstance');
                    if (!rteinstance) {
                        debugger; // FIXME
                    }
                    console.log("rteinstance", rteinstance);
                    console.log("origevent", event);
                    clickevent.preventDefault();
                    clickevent.stopPropagation();
                    const oldContent = rteinstance.getContent();
                    rteinstance.suspend();
                    showCreateDialog({
                        componentPath,
                        property,
                        oldContent,
                        writebackCallback: function (newvalue) {
                            rteinstance.setContent(newvalue);
                        },
                        isRichtext: true,
                        stackeddialog: true,
                        onFinishCallback: function () {
                            rteinstance.reactivate();
                            rteinstance.setContent(oldContent);
                            rteinstance.focus();
                            $('.cq-dialog-backdrop').removeClass('is-open');
                            $('.cq-dialog-backdrop').hide();
                        }
                    });
                });
            }
        });
    }

})
(jQuery, jQuery(document), this);

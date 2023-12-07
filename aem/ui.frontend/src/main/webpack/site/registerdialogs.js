/**
 * Registration of the custom dialogs.
 * Thanks to https://techforum.medium.com/how-to-connect-adobe-experience-manager-aem-with-chatgpt-312651291713 for hints!
 */

import {ContentCreationDialog} from './ContentCreationDialog.js';
import {SidePanelDialog} from './SidePanelDialog.js';
import {AIConfig} from './AIConfig.js';

try {
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
        const SERVICE_CREATE = 'create';
        const SERVICE_SIDEPANEL = 'sidepanel';
        const aiconfig = new AIConfig();
        var debug = true;

        channel.on('cq-sidepanel-loaded', (event) => Coral.commons.ready(event.target, loadSidebarPanelDialog));
        // for AEM 6.5.7 we, strangely, don't get the cq-sidepanel-loaded event. Try something else.
        channel.on('cq-layer-activated cui-contentloaded', (event) => Coral.commons.ready(event.target, loadSidebarPanelDialog));

        /**
         * Loads the Sidebar Panel AI if it wasn't loaded already and if there's a sidebar present.
         */
        function loadSidebarPanelDialog() {
            if (true) console.log("loadSidebarPanelDialog", arguments);
            try {
                aiconfig.ifEnabled(SERVICE_SIDEPANEL, undefined, () => {
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
                            if (debug) console.log("found dialog", dialog);
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
            if (debug) console.log("showCreateDialog", parameters);
            const dialogId = 'composumAI-create-dialog'; // possibly use editable.path to make it unique

            $.ajax({
                url: CREATE_DIALOG_URL + "?richtext=" + parameters.isRichtext,
                type: "GET",
                dataType: "html",
                success: function (data) {
                    if (debug) console.log("showCreateDialog ajax", data);
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
            '<coral-icon ' +
            '   class="coral-Form-fieldinfo coral3-Icon coral3-Icon--gearsEdit coral3-Icon--sizeS composum-ai-create-dialog-action" ' +
            '   title="AI Content Creation" icon="gearsEdit" alt="description" size="S" autoarialabel="on" role="img" ' +
            '   aria-label="AI Content Creation"></coral-icon>';

        /**
         * Inserts the AI content creation buttons for text areas in the provided element.
         * @param {HTMLElement} element - The DOM element to search for textareas.
         */
        function insertCreateButtonsForTextareas(element) {
            if (debug) console.log("insertCreateButton", arguments);
            if ($(element).find('.composum-ai-dialog').length > 0) {
                return; // don't insert buttons into our own dialog
            }
            $(element).find('div.coral-Form-fieldwrapper textarea.coral-Form-field[data-comp-ai-iconsadded!="true"]').each(
                function (index, textarea) {
                    if (debug) console.log("insertCreateButton textarea", textarea);
                    const resourceType = $(textarea).closest('coral-dialog-content').find('input[name="./sling:resourceType"]').val();
                    if (!resourceType) {
                        // debugger;
                    }
                    aiconfig.ifEnabled(SERVICE_CREATE, resourceType, () => {
                        const gearsEdit = $(fieldlabeliconHTML);
                        if ($(textarea.parentElement).find('coral-icon').length > 0) {
                            gearsEdit.addClass('composum-ai-iconshiftleft'); // help icon is there
                        }
                        gearsEdit.insertAfter(textarea);
                        textarea.setAttribute('data-comp-ai-iconsadded', 'true');
                        gearsEdit.click(function (event) {
                            if (debug) console.log("createButton click", arguments);
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
            if (debug) console.log("prepareDialog", event.type, event.target);
            try {
                aiconfig.ifEnabled(SERVICE_CREATE, undefined, () => {
                    Coral.commons.ready(event.target, function () {
                        if (debug) console.log("prepareDialog ready", event.type, event.target);
                        insertCreateButtonsForTextareas(event.target);
                    });
                });
            } catch (e) {
                console.error("error preparing dialog", event, e);
                debugger;
            }
        }

        channel.on('cq-layer-activated foundation-contentloaded cui-contentloaded', initRteHooks);


        /**
         * Initializes hooks for the richtext editor when activated.
         * @param {Event} event - The event indicating the activation of the RTE.
         */
        function initRteHooks(event) {
            if (debug) console.log("initRteHooks", event.type, event.target);
            try {
                channel.off('editing-start', onRteEditingStart)
                    .on('editing-start', onRteEditingStart);
                Granite?.author?.ContentFrame?.getDocument()
                    ?.off('editing-start', onRteEditingStart)
                    ?.on('editing-start', onRteEditingStart);
            } catch (e) {
                console.error("error initializing RTE hooks", event, e);
                debugger;
            }
        }

        const rtebuttonHTML = '<button is="coral-button" variant="quiet" class="rte-toolbar-item _coral-ActionButton composum-ai-create-dialog-action coral3-Button--quiet" type="button"\n' +
            '        title="AI Content Creation" icon="gearsEdit" size="S">\n' +
            '    <coral-icon size="S"\n' +
            '                class="_coral-Icon--sizeS _coral-Icon coral3-Icon--gearsEdit" role="img" icon="gearsEdit" alt="AI Content Creation"\n' +
            '                aria-label="AI Content Creation">\n' +
            '    </coral-icon>\n' +
            '</button>\n';

        /** editing-start event is received for an richtext editor - we have to register the button. */
        function onRteEditingStart(event) {
            aiconfig.ifEnabled(SERVICE_CREATE, undefined, () => {
                if (debug) console.log("onRteEditingStart", arguments);
                let $target = $(event.target);
                if ($target.closest('.composum-ai-dialog').length > 0) {
                    return; // don't insert buttons into our own dialog
                }
                let editable = Granite?.author?.selection?.getCurrentActive() ||
                    determineEditableFromElement(event.target); // when in content frame
                // we have to determine the componentPath and resourceType right now to see whether the button is enabled.
                // the rteinstance and the property name can also be determined later if we don't get them right now
                let componentPath = undefined;
                let resourceType = undefined;
                let propertyName = undefined;
                let rteinstance = $target.data('rteinstance') || $target.data('richText');
                if (!rteinstance) {
                    debugger; // FIXME
                }
                if (debug) console.log("onRteEditingStart rte found", event.type, event.target, editable);

                if (editable) {
                    componentPath = editable.path;
                    resourceType = editable.type;
                } else if ($target.closest('form.content-fragment-editor')[0]) {
                    let editorDiv = $target.closest('div#Editor');
                    componentPath = editorDiv.data('path');
                    // no resourcetype available on content fragments
                } else if ($target.closest('form.cq-dialog')[0]) {
                    let dialogForm = $target.closest('form.cq-dialog');
                    componentPath = dialogForm.attr('action');
                    resourceType = dialogForm.find('input[name="./sling:resourceType"]').val();
                    propertyName = $target.closest('div.richtext-container')
                        .find('input[type="hidden"][data-cq-richtext-input]').attr('name');
                }

                aiconfig.ifEnabled(SERVICE_CREATE, resourceType,
                    () => insertCreateButtons(event.target, editable, componentPath, resourceType, propertyName, rteinstance)
                );
            });
        }

        function insertCreateButtons(target, editable, componentPath, resourceType, propertyName, rteinstance) {
            if (debug) console.log("insertCreateButtons", arguments);
            let buttongroups = channel.find('#InlineEditingUI .rte-ui > div > coral-buttongroup, coral-dialog[fullscreen] .rte-ui > div > coral-buttongroup');
            const $target = $(target);
            if ($target.hasClass('cq-RichText-editable')) { // maximized editor
                buttongroups = buttongroups.add($target.parent().find('.rte-ui > div > coral-buttongroup').get());
            }
            if ($target.hasClass('cfm-multieditor-richtext-editor')) { // content fragment editor
                buttongroups = buttongroups.add($target.closest('div[data-form-view-container]').find('.rte-ui > div > coral-buttongroup'));
            }
            if (buttongroups.length === 0) {
                console.log("Warning: no buttongroups found", target, editable, componentPath, resourceType, propertyName, rteinstance);
            }
            buttongroups.each(function (index, buttongroup) {
                if ($(buttongroup).closest('.composum-ai-dialog').length === 0 &&
                    $(buttongroup).find('.composum-ai-create-dialog-action').length === 0) {
                    registerContentDialogInToolbar(buttongroup, target, editable, componentPath, resourceType, propertyName, rteinstance);
                }
            });
        }

        /** Registers the content creation dialog in the richtext editor toolbar */
        function registerContentDialogInToolbar(buttongroup, target, editable, componentPath, resourceType, propertyName, rteinstance) {
            if (debug) console.log("registerContentDialogInToolbar", arguments);
            const $button = $(rtebuttonHTML);
            $(buttongroup).append($button);
            $button.click(function (clickevent) {
                if (debug) console.log("createButtonText click", typeof clickevent.type, clickevent.target, editable, target, buttongroup);

                const rteproperty = $(buttongroup).closest('.richtext-container').find('[data-cq-richtext-editable=true]').attr('name');
                propertyName = rteproperty || propertyName;
                propertyName = propertyName && propertyName.startsWith('./') && propertyName.substring(2);
                propertyName = propertyName || 'text'; // normal case for a rte - when it's an inline rte in the content it's really hard to find out.

                clickevent.preventDefault();
                clickevent.stopPropagation();
                const oldContent = rteinstance.getContent();
                rteinstance.suspend();
                const backdropOpen = $('.cq-dialog-backdrop').hasClass('is-open');

                showCreateDialog({
                    componentPath,
                    property: propertyName,
                    oldContent,
                    writebackCallback: function (newvalue) {
                        rteinstance.setContent(newvalue);
                    },
                    isRichtext: true,
                    stackeddialog: true,
                    onFinishCallback: function () {
                        if (!backdropOpen) { // only hide if we weren't called from a dialog but an inline editor:
                            $('.cq-dialog-backdrop').removeClass('is-open').hide();
                        }
                        rteinstance.reactivate();
                        rteinstance.setContent(oldContent);
                        rteinstance.focus();
                    }
                });
            });
        }

        /** Determines the "most specific" editable containing the DOM element, in the sense of
         * editable.dom[0].contains(element).
         * This logic prevents a container being found instead of the contained editable. */
        function determineEditableFromElement(element) {
            if (debug) console.log("determineEditableFromElement", arguments);
            var bestEditable = undefined;
            for (var i = 0; i < Granite.author.editables.length; i++) {
                var editable = Granite.author.editables[i];
                // if it doesn't contain the element, continue
                if (!editable.dom[0].contains(element)) {
                    continue;
                }
                if (bestEditable && !bestEditable.dom[0].contains(editable.dom[0])) {
                    continue;
                }
                bestEditable = editable;
            }
            return bestEditable;
        }

    })
    (jQuery, jQuery(document), this);

} catch (e) {
    console.log('BUG: registerdialogs.js initialization error', e);
}

console.log("registerdialogs.js loaded");

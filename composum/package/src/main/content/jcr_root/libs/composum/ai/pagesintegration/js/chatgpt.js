/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
(function (window) {
    window.composum = window.composum || {};
    window.composum.pages = window.composum.pages || {};
    window.composum.pages.dialogs = window.composum.pages.dialogs || {};
    window.composum.pages.dialogs.const = window.composum.pages.dialogs.const || {};
    window.composum.pages.dialogs.const.dialogplugins = window.composum.pages.dialogs.const.dialogplugins || [];
    window.composum.chatgpt = window.composum.chatgpt || {};

    window.composum.pages.dialogs.const.dialogplugins.push(window.composum.chatgpt);

    (function (chatgpt, dialogs, pages, core, components) {
        'use strict';

        chatgpt.const = chatgpt.const || {};
        chatgpt.const.url = chatgpt.const.url || {};
        // all dialogs create their own subnodes in chatgpt.const.url to put that into several files.
        chatgpt.const.url.general = {
            authoring: '/bin/cpm/platform/ai/authoring',
            markdown: '/bin/cpm/platform/ai/approximated.markdown'
        };

        /** Will be called from Pages after a dialog is rendered via the dialogplugins hook.
         * Thus we bind ourselves into our buttons rendered into the dialog by the labelextension.jsp */
        chatgpt.dialogInitializeView = function (dialog, $element) {
            console.log('chatgpt.dialogInitializeView', dialog, $element);
            let $translationButtons = $element.find('.widget-ai-action.action-translate');
            $translationButtons.click(chatgpt.openTranslateDialog);
            let $categorizeButtons = $element.find('.widget-ai-action.action-pagecategories');
            $categorizeButtons.click(chatgpt.openCategorizeDialog);
            let $createButtons = $element.find('.widget-ai-action.action-create');
            $createButtons.click(chatgpt.openCreationDialog);
        };

        /** Looks for the actual text input or textarea that belongs to the labelextension. */
        chatgpt.searchInput = function ($labelextension) {
            var $formgroup = $labelextension.closest('div.form-group');
            var $input = $formgroup.find('input[type="text"],textarea');
            if ($input.length === 1) {
                return $input;
            }
            console.error('BUG! searchInput: no input found', $labelextension);
        };

        chatgpt.commonDialogInit = function ($el) {
            $el.find("button.maximize").click(chatgpt.maximizeRestoreFunc($el, true));
            $el.find("button.restore").click(chatgpt.maximizeRestoreFunc($el, false));
            $el.find("button.help").click(chatgpt.openHelpDialog.bind(this));
            chatgpt.addDragging($el);
        };

        chatgpt.maximizeRestoreFunc = function ($el, ismaximize) {
            let origcss;
            return function (event) {
                event.preventDefault();
                $el.toggleClass("dialog-size-maximized");
                // save original position and size of dialog which could have been modified by dragging
                const $dialog = $el.find(".modal-dialog");
                if (ismaximize) {
                    origcss = $dialog.css(["top", "left", "position"]);
                    $dialog.css("position", "");
                    $dialog.css("left", "");
                    $dialog.css("top", "");
                } else {
                    $dialog.css(origcss);
                }
                return false;
            };
        };

        chatgpt.openHelpDialog = function (event) {
            event.preventDefault();
            let $button = $(event.target);
            let url = $button.data('helpurl');
            core.openFormDialog(url, chatgpt.HelpDialog);
            return false;
        };

        /** A dialog showing help. */
        chatgpt.HelpDialog = components.LoadedDialog.extend({

            initialize: function (options) {
                components.LoadedDialog.prototype.initialize.call(this, options);
                chatgpt.commonDialogInit(this.$el);
            }

        });

        /** Adds dragging to the modal header. */
        chatgpt.addDragging = function ($el) {
            let dragging = false;
            let offset = {};
            let marginTop = 0;
            const $dialog = $el.find(".modal-dialog");
            const $header = $dialog.find(".modal-header");

            function handleMouseDown(e) {
                if ($(e.target).closest("button").length > 0 || e.which !== 1) {
                    return;
                }

                e.preventDefault();
                console.log("start dragging");
                if ($dialog.css('marginTop')) {
                    marginTop = parseInt($dialog.css('marginTop'), 10);
                }

                $dialog.css({
                    position: 'absolute',
                    left: $dialog.offset().left,
                    top: $dialog.offset().top - marginTop
                });

                dragging = true;
                offset = {
                    x: e.clientX - $dialog.offset().left,
                    y: e.clientY - $dialog.offset().top - marginTop
                };

                $(document).on("mousemove", handleMouseMove);
                $(document).on("mouseup", handleMouseUp);
            }

            function handleMouseMove(e) {
                if (dragging) {
                    e.preventDefault();
                    console.log("continue dragging: " + e.clientX + ", " + e.clientY);
                    $dialog.offset({
                        left: e.clientX - offset.x,
                        top: e.clientY - offset.y - marginTop
                    });
                }
            }

            function handleMouseUp(e) {
                if (dragging) {
                    e.preventDefault();
                    console.log("stop dragging");
                    dragging = false;
                    $(document).off("mousemove", handleMouseMove);
                    $(document).off("mouseup", handleMouseUp);
                }
            }

            $header.on("mousedown", handleMouseDown);
        };

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

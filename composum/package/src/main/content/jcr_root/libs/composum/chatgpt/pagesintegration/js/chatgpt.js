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
            authoring: '/bin/cpm/platform/chatgpt/authoring',
            markdown: '/bin/cpm/platform/chatgpt/approximated.markdown',
        };

        /** Will be called from Pages after a dialog is rendered via the dialogplugins hook.
         * Thus we bind ourselves into our buttons rendered into the dialog by the labelextension.jsp */
        chatgpt.dialogInitializeView = function (dialog, $element) {
            console.log('chatgpt.dialogInitializeView', dialog, $element);
            let $translationButtons = $element.find('.widget-chatgptaction.action-translate');
            $translationButtons.click(chatgpt.openTranslateDialog);
            let $categorizeButtons = $element.find('.widget-chatgptaction.action-pagecategories');
            $categorizeButtons.click(chatgpt.openCategorizeDialog);
            let $createButtons = $element.find('.widget-chatgptaction.action-create');
            $createButtons.click(chatgpt.openCreationDialog);
        }

        /** Looks for the actual text input or textarea that belongs to the labelextension. */
        chatgpt.searchInput = function ($labelextension) {
            var $formgroup = $labelextension.closest('div.form-group');
            var $input = $formgroup.find('input[type="text"],textarea');
            if ($input.length === 1) {
                return $input;
            }
            console.error('BUG! searchInput: no input found', $labelextension);
        }

        chatgpt.initButtons = function($el) {
            console.log('initButtons', $el);
            $el.find("button.maximize, .restore").click(function(event) {
                event.preventDefault();
                $el.toggleClass("dialog-size-maximized");
                return false;
            });
            $el.find("button.help").click(chatgpt.openHelpDialog.bind(this));
        }

        chatgpt.openHelpDialog = function(event) {
            event.preventDefault();
            let $button = $(event.target);
            let url = $button.data('helpurl');
            core.openFormDialog(url, chatgpt.HelpDialog);
            return false;
        }

        /** A dialog showing help. */
        chatgpt.HelpDialog = components.LoadedDialog.extend({
            // no functions so far.
        });

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

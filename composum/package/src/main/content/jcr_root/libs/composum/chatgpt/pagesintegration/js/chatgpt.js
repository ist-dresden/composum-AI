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

    (function (chatgpt, dialogs, pages, core) {
        'use strict';

        chatgpt.const = {
            url: {
                authoring: '/bin/cpm/platform/chatgpt/authoring',
                markdown: '/bin/cpm/platform/chatgpt/approximated.markdown',
            }
        };

        chatgpt.dialogInitializeView = function (dialog, $element) {
            console.log('chatgpt.dialogInitializeView', dialog, $element);
            // alert('chatgpt.dialogInitializeView' + dialog + $element);
        }

        dialogs.registerDialogPlugin({dialogInitializeView: chatgpt.dialogInitializeView});

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core);

})(window);

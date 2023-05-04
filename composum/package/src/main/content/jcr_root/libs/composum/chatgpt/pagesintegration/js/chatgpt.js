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

        chatgpt.const = {
            url: {
                authoring: '/bin/cpm/platform/chatgpt/authoring',
                markdown: '/bin/cpm/platform/chatgpt/approximated.markdown',
                translationDialog: '/bin/cpm/platform/chatgpt/dialog.translationDialog.html'
            }
        };

        chatgpt.dialogInitializeView = function (dialog, $element) {
            console.log('chatgpt.dialogInitializeView', dialog, $element);
            let $translationButtons = $element.find('.widget-chatgptaction.action-translate');
            $translationButtons.click(chatgpt.openTranslateDialog);
            // alert('chatgpt.dialogInitializeView' + dialog + $element);
        }

        /**
         * Dialog for translation
         * @param options{path,propertyName}
         */
        // as example see replication.PublishDialog
        chatgpt.TranslationDialog = components.LoadedDialog.extend({

            initialize: function (options) {
                components.LoadedDialog.prototype.initialize.call(this, options);
                this.path = options.data.path;
                this.propertyName = options.data.propertyName;
                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
                this.$el.on('hidden.bs.modal', _.bind(this.onHidden, this));
                this.$el.find('.btn-primary.accept').click(_.bind(this.save, this));
                this.$form = this.$el.find('form');
                this.$translation = this.$el.find('.translation');
                this.$languageSelects = this.$el.find('.language-select-radio')
                this.$languageSelects.on('change', _.bind(this.languageChanged, this));
            },

            languageChanged: function (event) {
                var language = $(event.target).val();
                console.log('languageChanged', language, arguments);
                this.translate(language);
            },

            onShown: function (event) {
                console.log('onShown', arguments);
                if (this.$languageSelects.length == 1) {
                    this.translate(this.$languageSelects.first().val());
                }
            },

            save: function (event) {
                event.preventDefault();
                console.log('save', arguments);
                return false;
            },

            onHidden: function (event) {
                this.abort(event);
            },

            abort: function (event) {
                // todo abort the request if still running
                event.preventDefault();
                console.error('abort', arguments);
                return false;
            },

            translate(language) {
                // todo abort if a translation is still running for a different language
                // todo translate the text if no translation is running for the language
                console.log('translate', arguments);
                // todo start spinner
                this.$translation.val('translating ' + language);
            }
        });

        chatgpt.openTranslateDialog = function (event) {
            var path = $(event.target).data('path');
            var property = $(event.target).data('property');
            var url = chatgpt.const.url.translationDialog + core.encodePath(path + '/' + property);
            core.openFormDialog(url, chatgpt.TranslationDialog, {data: {path: path, propertyName: property}},
                function () {
                    console.log('initview', arguments);
                }, function () {
                    console.log('callback', arguments);
                }
            ); // todo other parameters? initview, callback?
        }

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

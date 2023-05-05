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
                this.$pathfield = this.$el.find('input[name="path"]');
                this.$propertyfield = this.$el.find('input[name="property"]');
                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
                this.$el.on('hidden.bs.modal', _.bind(this.onHidden, this));
                this.$el.find('.btn-primary.accept').click(_.bind(this.save, this));
                this.$form = this.$el.find('form');
                this.$translation = this.$el.find('.translation');
                this.$languageSelects = this.$el.find('.language-select-radio')
                this.$languageSelects.on('change', _.bind(this.languageChanged, this));
                this.$alert = this.$el.find('.alert');
                this.$spinner = this.$el.find('.loading-curtain');

                if (this.$languageSelects.length == 1) {
                    this.translate(this.$languageSelects.first().val());
                }
            },

            languageChanged: function (event) {
                var language = $(event.target).val();
                console.log('languageChanged', language, arguments);
                this.translate(language);
            },

            onShown: function (event) {
                console.log('onShown', arguments);
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
                // TODO abort the request if still running
                event.preventDefault();
                console.error('abort', arguments);
                return false;
            },

            translate(language) {
                var that = this;

                function abortRunningCalls() {
                    if (that.runningxhr) {
                        that.runningxhr.abort();
                        that.runningxhr = undefined;
                    }
                }

                function consumeXhr(xhr) {
                    abortRunningCalls();
                    that.runningxhr = xhr;
                }

                console.log('translate', arguments);
                // ajaxPost: function (url, data, config, onSuccess, onError, onComplete)
                let url = chatgpt.const.url.authoring + ".translate.json";
                core.ajaxPost(url, {
                        sourceLanguage: language,
                        path: this.$pathfield.val(),
                        property: this.$propertyfield.val()
                    }, {dataType: 'json', xhrconsumer: consumeXhr},
                    _.bind(this.onTranslation, this), _.bind(this.onError, this));
                this.$spinner.show();
                this.$translation.hide();
            },

            onTranslation: function (status) {
                // TODO handle text and HTML differently
                if (status && status.status >= 200 && status.status < 300 && status.data && status.data.result && status.data.result.translation) {
                    this.$translation.html(status.data.result.translation[0]);
                    this.$alert.hide();
                    this.$spinner.hide();
                    this.$translation.show();
                } else {
                    onError(null, status);
                }
            },

            onError: function (xhr, status) {
                console.error('onError', arguments);
                // TODO sensible handling of errors
                this.$alert.text(xhr + " / " + status);
                this.$alert.show();
                this.$spinner.hide();
                this.$translation.hide();
            }

        });

        chatgpt.openTranslateDialog = function (event) {
            var path = $(event.target).data('path');
            var property = $(event.target).data('property');
            var url = chatgpt.const.url.translationDialog + core.encodePath(path + '/' + property);
            core.openFormDialog(url, chatgpt.TranslationDialog, {},
                function () {
                    console.log('initview', arguments);
                }, function () {
                    console.log('callback', arguments);
                }
            ); // TODO other parameters? initview, callback?
        }

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

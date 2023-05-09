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
                this.$accept = this.$el.find('.btn-primary.accept');
                this.$form = this.$el.find('form');
                this.$translation = this.$el.find('.translation');
                this.$languageSelects = this.$el.find('.language-select-radio')
                this.$alert = this.$el.find('.alert');
                this.$spinner = this.$el.find('.loading-curtain');
                this.$outputfield = options.outputfield;

                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
                this.$el.on('hidden.bs.modal', _.bind(this.onHidden, this));
                this.$accept.click(_.bind(this.accept, this));
                this.$languageSelects.on('change', _.bind(this.languageChanged, this));

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

            accept: function (event) {
                event.preventDefault();
                console.log('accept', arguments);
                if (this.$outputfield.hasClass('trumbowyg-textarea')) {
                    this.$outputfield.trumbowyg('html', this.$translation.html());
                } else {
                    this.$outputfield.val(this.$translation.text());
                }
                this.destroy();
                return false;
            },

            onHidden: function (event) {
                this.abort(event);
            },

            abort: function (event) {
                console.error('abort', arguments);
                event.preventDefault();
                this.abortRunningCalls();
                return false;
            },

            translate(language) {
                var that = this;

                function consumeXhr(xhr) {
                    that.abortRunningCalls();
                    that.runningxhr = xhr;
                }

                console.log('translate', arguments);
                this.setTranslating();
                let url = chatgpt.const.url.authoring + ".translate.json";
                core.ajaxPost(url, {
                        sourceLanguage: language,
                        path: this.$pathfield.val(),
                        property: this.$propertyfield.val()
                    }, {dataType: 'json', xhrconsumer: consumeXhr},
                    _.bind(this.onTranslation, this), _.bind(this.onError, this));
            },

            abortRunningCalls: function () {
                if (this.runningxhr) {
                    this.runningxhr.abort();
                    this.runningxhr = undefined;
                }
            },

            onTranslation: function (status) {
                if (status && status.status >= 200 && status.status < 300 && status.data && status.data.result && status.data.result.translation) {
                    this.$translation.html(status.data.result.translation[0]);
                    this.setTranslated();
                } else {
                    onError(null, status);
                }
            },

            setTranslated: function () {
                this.$alert.hide();
                this.$spinner.hide();
                if (this.$translation.text()) {
                    this.$translation.show();
                    this.$accept.prop('disabled', false);
                } else {
                    this.$translation.hide();
                    this.$accept.prop('disabled', true);
                }
            },

            onError: function (xhr, status) {
                console.error('onError', arguments);
                // TODO sensible handling of errors
                this.$alert.text(xhr.status + " " + xhr.statusText + " : " + xhr.responseText + " / " + status);
                this.$alert.show();
                this.$spinner.hide();
                this.$translation.hide();
                this.$accept.prop('disabled', true);
            },

            setTranslating: function () {
                this.$alert.hide();
                this.$spinner.show();
                this.$translation.hide();
                this.$accept.prop('disabled', true);
            }

        });

        chatgpt.openTranslateDialog = function (event) {
            let $target = $(event.target);
            var path = $target.data('path');
            var property = $target.data('property');
            var propertypath = $target.data('propertypath');
            var url = chatgpt.const.url.translationDialog + core.encodePath(path + '/' + property) +
                "?propertypath=" + encodeURIComponent(propertypath) + "&pages.locale=" + pages.getLocale();
            core.openFormDialog(url, chatgpt.TranslationDialog, {outputfield: chatgpt.searchInput($target)});
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

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

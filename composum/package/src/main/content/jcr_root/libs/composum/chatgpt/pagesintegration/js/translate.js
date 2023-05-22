/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
(function (window) {
    window.composum = window.composum || {};
    window.composum.chatgpt = window.composum.chatgpt || {};

    (function (chatgpt, dialogs, pages, core, components) {
        'use strict';

        chatgpt.const = chatgpt.const || {};
        chatgpt.const.url = chatgpt.const.url || {};
        chatgpt.const.url.translate = {
            translationDialog: '/bin/cpm/platform/chatgpt/dialog.translationDialog.html'
        };

        /**
         * Dialog for translation
         * @param options{path,propertyName}
         */
        // as example see replication.PublishDialog
        chatgpt.TranslationDialog = components.LoadedDialog.extend({

            initialize: function (options) {
                components.LoadedDialog.prototype.initialize.call(this, options);
                chatgpt.initButtons(this.$el);
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

                if (this.$languageSelects.length === 1) {
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
                let widget = core.widgetOf(this.$outputfield);
                if (widget) {
                    if (widget.richText) {
                        widget.setValue(this.$translation.html());
                    } else {
                        widget.setValue(this.$translation.text());
                    }
                } else {
                    console.error("Bug: cannot find widget for ", this.$outputfield);
                }
                this.$el.modal('hide');
                if (widget) {
                    widget.grabFocus();
                }
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
                let url = chatgpt.const.url.general.authoring + ".translate.json";
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
                    this.onError(null, status);
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
            var url = chatgpt.const.url.translate.translationDialog + core.encodePath(path + '/' + property) +
                "?propertypath=" + encodeURIComponent(propertypath) + "&pages.locale=" + pages.getLocale();
            core.openFormDialog(url, chatgpt.TranslationDialog, {outputfield: chatgpt.searchInput($target)});
        }

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

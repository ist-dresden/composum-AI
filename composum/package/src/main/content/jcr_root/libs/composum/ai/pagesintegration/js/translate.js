/*
 * copyright (c) 2015ff IST GmbH Dresden, Germany - https://www.ist-software.com
 *
 * This software may be modified and distributed under the terms of the MIT license.
 */
(function (window) {
    window.composum = window.composum || {};
    window.composum.ai = window.composum.ai || {};

    (function (ai, dialogs, pages, core, components) {
        'use strict';

        ai.const = ai.const || {};
        ai.const.url = ai.const.url || {};
        ai.const.url.translate = {
            translationDialog: '/bin/cpm/platform/ai/dialog.translationDialog.html'
        };

        /**
         * Dialog for translation
         * @param options{path,propertyName, widget}
         */
        // as example see replication.PublishDialog
        ai.TranslationDialog = components.LoadedDialog.extend({

            initialize: function (options) {
                components.LoadedDialog.prototype.initialize.call(this, options);
                ai.commonDialogInit(this.$el);
                this.$pathfield = this.$el.find('input[name="path"]');
                this.$propertyfield = this.$el.find('input[name="property"]');
                this.$accept = this.$el.find('.btn-primary.accept');
                this.$translation = this.$el.find('.translation');
                this.$languageSelects = this.$el.find('.language-select-radio')
                this.$alert = this.$el.find('.generalalert');
                this.$truncationalert = this.$el.find('.truncationalert');
                this.$spinner = this.$el.find('.loading-curtain');
                this.widget = options.widget;
                this.isRichText = options.isRichText;

                this.$el.on('shown.bs.modal', _.bind(this.onShown, this));
                this.$el.on('hidden.bs.modal', _.bind(this.onHidden, this));
                this.$accept.click(_.bind(this.accept, this));
                this.$languageSelects.on('change', _.bind(this.languageChanged, this));
                this.streaming = typeof (EventSource) !== "undefined";

                if (this.$languageSelects.length === 1) {
                    this.translate(this.$languageSelects.first().val());
                }
            },

            languageChanged: function (event) {
                var language = $(event.target).val();
                console.log('languageChanged', language, arguments);
                this.translate(language);
            },

            accept: function (event) {
                event.preventDefault();
                console.log('accept', arguments);
                if (this.isRichText) {
                    this.widget.setValue(this.$translation.html());
                } else {
                    this.widget.setValue(this.$translation.text());
                }
                this.$el.modal('hide');
                this.widget.grabFocus();
                return false;
            },

            onHidden: function (event) {
                this.abort(event);
            },

            abort: function (event) {
                console.error('abort', arguments);
                event.preventDefault();
                this.abortRunningCalls();
                this.$spinner.hide();
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
                let url = ai.const.url.general.authoring + ".translate.json";
                core.ajaxPost(url, {
                        sourceLanguage: language,
                        path: this.$pathfield.val(),
                        richText: this.isRichText,
                        streaming: this.streaming,
                        property: this.$propertyfield.val()
                    }, {dataType: 'json', xhrconsumer: consumeXhr},
                    _.bind(this.onTranslation, this), _.bind(this.onError, this));
            },

            abortRunningCalls: function () {
                if (this.runningxhr) {
                    this.runningxhr.abort();
                    this.runningxhr = undefined;
                }
                if (this.eventSource) {
                    this.eventSource.close();
                    this.eventSource = undefined;
                }
            },

            /** When a non-streaming translation is finished. */
            onTranslation: function (status) {
                const statusOk = status && status.status >= 200 && status.status < 300 && status.data && status.data.result;

                if (statusOk && status.data.result.translation) {
                    let translationResult = status.data.result.translation[0];
                    this.updateTranslation(translationResult);
                    this.setTranslated();
                } else if (statusOk && status.data.result.streamid) {
                    const streamid = status.data.result.streamid;
                    this.startStreaming(streamid);
                } else {
                    this.onError(null, status);
                }
            },

            updateTranslation(translation) {
                if (this.isRichText) {
                    this.$translation.html(translation);
                } else {
                    this.$translation.text(translation);
                }
            },

            setTranslated: function () {
                this.abortRunningCalls();
                this.$spinner.hide();
                if (this.$translation.text()) {
                    this.$accept.prop('disabled', false);
                } else {
                    this.$accept.prop('disabled', true);
                }
            },

            onError: function (xhr, status) {
                console.error('onError', arguments);
                // TODO sensible handling of errors
                let alert = xhr && xhr.status + " " + xhr.statusText + " : " + xhr.responseText + " / " + status || status;
                this.$alert.text(alert);
                this.$alert.show();
                this.abortRunningCalls();
                this.$spinner.hide();
                this.$accept.prop('disabled', true);
            },

            setTranslating: function () {
                this.$alert.hide();
                this.$alert.text('');
                this.$spinner.show();
                this.$translation.html("");
                this.$truncationalert.hide();
                this.$accept.prop('disabled', true);
            },

            startStreaming: function (streamid) {
                console.log('startStreaming', arguments);
                let url = ai.const.url.general.authoring + ".streamresponse.sse";
                this.abortRunningCalls();
                this.streamingResult = "";
                this.eventSource = new EventSource(url + "?streamid=" + streamid);
                this.eventSource.onmessage = this.onStreamingMessage.bind(this);
                this.eventSource.onerror = this.onStreamingError.bind(this);
                this.eventSource.addEventListener('finished', this.onStreamingFinished.bind(this));
                this.eventSource.addEventListener('exception', this.onStreamingException.bind(this));
            },

            onStreamingMessage: function (event) {
                console.log('onStreamingMessage', arguments);
                this.streamingResult += JSON.parse(event.data);
                this.updateTranslation(this.streamingResult);
            },

            onStreamingFinished: function (event) {
                console.log('onStreamingFinished', arguments);
                this.eventSource.close();
                this.setTranslated();
                const status = JSON.parse(event.data);
                console.log(status);
                const statusOk = status && status.status >= 200 && status.status < 300 && status.data && status.data.result && status.data.result.finishreason;
                if (statusOk) {
                    const finishreason = status.data.result.finishreason;
                    if (finishreason === 'STOP') {
                        this.$truncationalert.hide();
                    } else if (finishreason === 'LENGTH') {
                        this.$truncationalert.show();
                    } else {
                        console.error('BUG: Unknown finishreason: ' + finishreason);
                    }
                }
            },

            /** Exception on the server side. */
            onStreamingException: function (event) {
                console.log('onStreamingException', arguments);
                this.eventSource.close();
                this.abortRunningCalls();
                this.$spinner.hide();
                this.$alert.text(event.data);
                this.$alert.show();
            },

            onStreamingError: function (event) {
                console.log('onStreamingError', arguments);
                this.eventSource.close();
                this.abortRunningCalls();
                this.$spinner.hide();
                this.$alert.text('Connection failed.');
                this.$alert.show();
            }

        });

        ai.openTranslateDialog = function (event) {
            let $target = $(event.target);
            var path = $target.data('path');
            var property = $target.data('property');
            var propertypath = $target.data('propertypath');
            let outputfield = ai.searchInput($target);
            let widget = core.widgetOf(outputfield);
            if (!widget) {
                console.error("Bug: cannot find widget for ", this.$outputfield);
                throw "Bug: cannot find widget for " + this.$outputfield;
            }
            let isRichText = !!widget.richText;
            var url = ai.const.url.translate.translationDialog + core.encodePath(path + '/' + property) +
                "?propertypath=" + encodeURIComponent(propertypath) + "&pages.locale=" + pages.getLocale() + "&richtext=" + isRichText;
            core.openFormDialog(url, ai.TranslationDialog, {widget: widget, isRichText: isRichText});
        }

    })(window.composum.ai, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

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
        ai.const.url.create = {
            createDialog: '/bin/cpm/platform/ai/dialog.creationDialog.html'
        }

        ai.openCreationDialog = function (event) {
            let $target = $(event.target);
            let path = $target.data('path');
            let pagePath = $target.data('pagepath');
            let property = $target.data('property');
            let propertypath = $target.data('propertypath');
            let outputfield = ai.searchInput($target);
            let widget = core.widgetOf(outputfield);
            let isRichText = widget && !!widget.richText;
            let url = ai.const.url.create.createDialog + core.encodePath(path + '/' + property) +
                "?propertypath=" + encodeURIComponent(propertypath) + "&pages.locale=" + pages.getLocale() + "&richtext=" + isRichText;
            core.openFormDialog(url, ai.CreateDialog, {
                widget: widget, isRichText: isRichText,
                componentPath: path, pagePath: pagePath, componentPropertyPath: path + '/' + property
            });
        }

        /** Maps widget paths to the saved state for the dialog. */
        ai.createDialogStates = {};

        /**
         * Content creation dialog.
         * @param options{widget, isRichText, componentPath, pagePath, componentPropertyPath}
         */
        ai.CreateDialog = core.components.FormDialog.extend({

            initialize: function (options) {
                core.components.FormDialog.prototype.initialize.apply(this, [options]);
                ai.commonDialogInit(this.$el);
                this.widget = options.widget;
                this.isRichText = options.isRichText;
                this.componentPath = options.componentPath;
                this.pagePath = options.pagePath;
                this.componentPropertyPath = options.componentPropertyPath;
                this.streaming = typeof (EventSource) !== "undefined";

                this.$predefinedPrompts = this.$el.find('.predefined-prompts');
                this.$contentSelect = this.$el.find('.content-selector');
                this.$textLength = this.$el.find('.text-length-selector');
                this.$prompt = this.$el.find('.prompt-textarea');
                this.$alert = this.$el.find('.generalalert');
                this.$truncationalert = this.$el.find('.truncationalert');

                this.$spinner = this.$el.find('.loading-indicator');
                this.$response = this.$el.find('.ai-response-field');

                this.$el.find('.back-button').click(this.backButtonClicked.bind(this));
                this.$el.find('.forward-button').click(this.forwardButtonClicked.bind(this));
                this.$el.find('.generate-button').click(this.generateButtonClicked.bind(this));
                this.$el.find('.reset-button').click(this.resetButtonClicked.bind(this));

                this.$el.find('.predefined-prompts').change(this.predefinedPromptsChanged.bind(this));
                this.$prompt.change(this.promptChanged.bind(this));

                this.$el.find('.replace-button').click(this.replaceButtonClicked.bind(this));
                this.$el.find('.append-button').click(this.appendButtonClicked.bind(this));
                this.$el.find('.cancel-button').click(this.cancelButtonClicked.bind(this));
                this.$el.on('hidden.bs.modal', this.abortRunningCalls.bind(this));

                if (!this.widget) {
                    console.log('No widget found for ', this.componentPropertyPath);
                    this.$alert.show();
                    this.$alert.text('Bug, please report: no widget found for ' + this.componentPropertyPath);
                }

                this.history = ai.createDialogStates[this.componentPropertyPath];
                console.log('History for ', this.componentPropertyPath, ' used.'); // FIXME remove this.
                if (!this.history) {
                    this.history = [];
                    ai.createDialogStates[this.componentPropertyPath] = this.history;
                }
                this.historyPosition = this.history.length - 1;
                if (this.historyPosition >= 0) {
                    this.restoreStateFromMap(this.history[this.historyPosition]);
                }

                if (this.isRichText) {
                    core.widgetOf(this.$response.find('textarea')).onChange(this.adjustButtonStates.bind(this));
                } else {
                    this.$response.change(this.adjustButtonStates.bind(this));
                }

                this.$el.find('#promptTextarea').mouseleave(this.adjustButtonStates.bind(this));
                this.$el.find('.generate-container').mouseenter(this.adjustButtonStates.bind(this));

                this.adjustButtonStates();
            },

            adjustButtonStates: function () {
                this.$el.find('.back-button').prop('disabled', this.historyPosition <= 0);
                this.$el.find('.forward-button').prop('disabled', this.historyPosition >= this.history.length - 1);
                this.$el.find('.generate-button').prop('disabled', !this.$prompt.val());
                let responseVal;
                if (this.isRichText) {
                    responseVal = core.widgetOf(this.$response.find('textarea')).getValue();
                } else {
                    responseVal = this.$response.val();
                }
                this.$el.find('.replace-button').prop('disabled', !responseVal);
                this.$el.find('.append-button').prop('disabled', !responseVal);
            },

            /** Creates a map that saves the content of all fields of this dialog. */
            makeSaveStateMap: function () {
                return {
                    'predefinedPrompts': this.$predefinedPrompts.val(),
                    'contentSelect': this.$contentSelect.val(),
                    'textLength': this.$textLength.val(),
                    'prompt': this.$prompt.val(),
                    'result': this.getResult()
                };
            },

            saveState: function () {
                // save if we are at the last position and the current state is different from the last saved state
                let currentState = this.makeSaveStateMap();
                let lastSavedState = this.history[this.historyPosition];
                if (!_.isEqual(currentState, lastSavedState)) {
                    console.log("SAVING STATE!");
                    this.history.push(currentState);
                    this.historyPosition = this.history.length - 1;
                }
                console.log('saveState', this.historyPosition, this.history.length, this.history);
                this.adjustButtonStates();
            },

            /** Restores the state of this dialog from the given map. */
            restoreStateFromMap: function (map) {
                this.$predefinedPrompts.val(map['predefinedPrompts']);
                this.$contentSelect.val(map['contentSelect']);
                this.$textLength.val(map['textLength']);
                this.$prompt.val(map['prompt']);
                this.setResult(map['result']);
                this.adjustButtonStates();
            },

            /** Button 'Reset' was clicked. */
            resetButtonClicked: function (event) {
                event.preventDefault();
                this.saveState();
                this.restoreStateFromMap({});
                return false;
            },

            predefinedPromptsChanged: function (event) {
                event.preventDefault();
                let predefinedPrompt = this.$predefinedPrompts.val();
                this.$prompt.val(predefinedPrompt);
                this.adjustButtonStates();
                return false;
            },

            promptChanged: function (event) {
                event.preventDefault();
                this.$predefinedPrompts.val(this.$predefinedPrompts.find('option:first').val());
                this.adjustButtonStates();
                return false;
            },

            backButtonClicked: function (event) {
                this.saveState();
                if (this.historyPosition > 0) {
                    this.historyPosition = this.historyPosition - 1;
                    let lastSavedState = this.history[this.historyPosition];
                    console.log('switching to state', this.historyPosition, this.history.length)
                    this.restoreStateFromMap(lastSavedState);
                }
            },

            forwardButtonClicked: function (event) {
                this.saveState();
                if (this.historyPosition < this.history.length - 1) {
                    this.historyPosition = this.historyPosition + 1;
                    let lastSavedState = this.history[this.historyPosition];
                    console.log('switching to state', this.historyPosition, this.history.length)
                    this.restoreStateFromMap(lastSavedState);
                }
            },

            setLoading: function (loading) {
                if (loading) {
                    this.$spinner.show();
                    this.$alert.hide();
                    this.$alert.text('');
                } else {
                    this.$spinner.hide();
                }
            },

            generateButtonClicked: function (event) {
                event.preventDefault();
                this.setLoading(true);

                const that = this;

                function consumeXhr(xhr) {
                    that.abortRunningCalls();
                    that.runningxhr = xhr;
                }

                let contentSelect = this.$contentSelect.val();
                let textLength = this.$textLength.val();
                let prompt = this.$prompt.val();
                var inputText;
                var inputPath;
                if (contentSelect === 'widget') {
                    inputText = this.widget.getValue();
                } else if (contentSelect === 'page') {
                    inputPath = this.pagePath;
                } else if (contentSelect === 'component') {
                    inputPath = this.componentPath;
                } else if (contentSelect === 'lastoutput') {
                    inputText = this.getResult();
                }

                let url = ai.const.url.general.authoring + ".create.json";
                core.ajaxPost(url, {
                        contentSelect: contentSelect,
                        textLength: textLength,
                        inputText: inputText,
                        inputPath: inputPath,
                        streaming: this.streaming,
                        richText: this.isRichText,
                        prompt: prompt
                    }, {dataType: 'json', xhrconsumer: consumeXhr},
                    this.generateSuccess.bind(this), this.generateError.bind(this));
                return false;
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

            generateSuccess: function (data) {
                this.setLoading(false);
                const statusOK = data.status && data.status >= 200 && data.status < 300 && data.data && data.data.result;
                if (statusOK && data.data.result.text) {
                    console.log("Success generating text: ", data);
                    let value = data.data.result.text;
                    this.setResult(value);
                    this.saveState();
                } else if (statusOK && data.data.result.streamid) {
                    const streamid = data.data.result.streamid;
                    this.startStreaming(streamid);
                } else {
                    console.error("Error generating text: ", data);
                    this.$alert.html("Error generating text: " + JSON.stringify(data));
                    this.$alert.show();
                }
            },

            setResult: function (value) {
                if (this.isRichText) {
                    core.widgetOf(this.$response.find('textarea')).setValue(value);
                    this.$response.attr('data-fullresponse', value); // for debugging
                } else {
                    this.$response.val(value);
                }
                this.adjustButtonStates();
            },

            getResult: function () {
                if (this.isRichText) {
                    return core.widgetOf(this.$response.find('textarea')).getValue();
                } else {
                    return this.$response.val();
                }
            },

            generateError: function (jqXHR, textStatus, errorThrown) {
                this.setLoading(false);
                console.error("Error generating text: ", arguments);
                this.$alert.html("Error generating text: " + JSON.stringify(arguments));
                this.$alert.show();
            },

            replaceButtonClicked: function (event) {
                event.preventDefault();
                const result = this.getResult();
                if (this.isRichText) {
                    this.widget.setValue(result);
                } else {
                    this.widget.setValue(result);
                }
                this.$el.modal('hide');
                this.widget.grabFocus();
                return false;
            },

            appendButtonClicked: function (event) {
                event.preventDefault();
                const result = this.getResult();
                let previousValue = this.widget.getValue();
                previousValue = previousValue ? previousValue.trim() + "\n\n" : "";
                if (this.isRichText) {
                    this.widget.setValue(previousValue + "<p>" + result + "</p>");
                } else {
                    this.widget.setValue(previousValue + "\n\n" + result);
                }
                this.$el.modal('hide');
                this.widget.grabFocus();
                return false;
            },

            cancelButtonClicked: function (event) {
                event.preventDefault();
                this.saveState();
                this.$el.modal('hide');
                return false;
            },

            startStreaming: function (streamid) {
                console.log("Start streaming: ", arguments);
                let url = ai.const.url.general.authoring + ".streamresponse.sse";
                this.abortRunningCalls();
                this.setLoading(true);
                this.streamingResult = "";
                this.eventSource = new EventSource(url + "?streamid=" + streamid);
                this.eventSource.onmessage = this.onStreamingMessage.bind(this, this.eventSource);
                this.eventSource.onerror = this.onStreamingError.bind(this, this.eventSource);
                this.eventSource.addEventListener('finished', this.onStreamingFinished.bind(this));
                this.eventSource.addEventListener('exception', this.onStreamingException.bind(this));
            },

            onStreamingMessage: function (eventSource, event) {
                // console.log('onStreamingMessage', arguments);
                this.streamingResult += JSON.parse(event.data);
                this.setResult(this.streamingResult);
            },

            onStreamingError: function (eventSource, event) {
                console.log('onStreamingError', arguments);
                this.eventSource.close();
                this.abortRunningCalls();
                this.setLoading(false);
                this.$alert.text('Connection failed.');
                this.$alert.show();
            },

            onStreamingFinished: function (event) {
                console.log('onStreamingFinished', arguments);
                this.eventSource.close();
                this.abortRunningCalls();
                this.setLoading(false);
                this.saveState();
                const status = JSON.parse(event.data);
                console.log(status);
                const statusOk = status && status.status >= 200 && status.status < 300 && status.data && status.data.result && status.data.result.finishreason;
                if (statusOk) {
                    const finishreason = status.data.result.finishreason;
                    if (finishreason === 'STOP') {
                        this.$truncationalert.hide();
                    } else if (finishreason == 'LENGTH') {
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
                this.setLoading(false);
                this.abortRunningCalls();
                this.$alert.text(event.data);
                this.$alert.show();
            }

        });


    })(window.composum.ai, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

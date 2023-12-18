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
            createDialog: '/bin/cpm/ai/dialog.creationDialog.html',
            approximated: '/bin/cpm/ai/approximated'
        }

        ai.openCreationDialog = _.debounce(function (event) {
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
            let isNew = $target.closest("form").attr("action") && $target.closest("form").attr("action").endsWith("/*");
            core.openFormDialog(url, ai.CreateDialog, {
                widget: widget, isRichText: isRichText,
                componentPath: path, pagePath: pagePath, componentPropertyPath: path + '/' + property, isNew
            });
        }, 1000, true);

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
                this.isNew = options.isNew;

                this.$predefinedPrompts = this.$el.find('.predefined-prompts');
                this.$contentSelect = this.$el.find('.content-selector');
                this.$textLength = this.$el.find('.text-length-selector');
                this.$prompt = this.$el.find('.prompt-textarea');
                this.$alert = this.$el.find('.generalalert');
                this.$truncationalert = this.$el.find('.truncationalert');

                this.$spinner = this.$el.find('.loading-indicator');
                this.$response = this.$el.find('.ai-response-field');
                this.$sourceContent = this.$el.find('.ai-source-field');

                this.$urlContainer = this.$el.find('.composum-ai-url-container');
                this.$urlField = this.$el.find('.composum-ai-url-field');

                this.setSourceContent(this.widget.getValue());

                this.$el.find('.back-button').click(this.backButtonClicked.bind(this));
                this.$el.find('.forward-button').click(this.forwardButtonClicked.bind(this));
                this.$el.find('.generate-button').click(this.generateButtonClicked.bind(this)).prop('disabled', true);
                this.$el.find('.stop-button').click(this.stopButtonClicked.bind(this));
                this.$el.find('.reset-button').click(this.resetButtonClicked.bind(this));
                this.$el.find('.reset-history-button').click(this.resetHistoryButtonClicked.bind(this));

                this.$el.find('.predefined-prompts').change(this.predefinedPromptsChanged.bind(this));
                this.$prompt.change(this.promptChanged.bind(this));

                this.$contentSelect.change(this.contentSelectChanged.bind(this));
                this.$sourceContent.change(this.sourceChanged.bind(this));
                this.$urlField.change(this.urlChanged.bind(this));
                this.$urlField.keydown((event) => {
                    if (event.which === 13 && !event.shiftKey && !event.ctrlKey && !event.altKey && !event.metaKey) {
                        event.preventDefault();
                        this.urlChanged(event);
                    }
                });

                this.$el.find('.replace-button').click(this.replaceButtonClicked.bind(this));
                this.$el.find('.append-button').click(this.appendButtonClicked.bind(this));
                this.$el.find('.cancel-button').click(this.cancelButtonClicked.bind(this));
                this.$el.on('hidden.bs.modal', this.abortRunningCalls.bind(this));

                if (!this.widget) {
                    console.log('No widget found for ', this.componentPropertyPath);
                    this.$alert.show();
                    this.$alert.text('Bug, please report: no widget found for ' + this.componentPropertyPath);
                }

                if (!this.isNew) {
                    this.history = ai.createDialogStates[this.componentPropertyPath];
                    if (!this.history) {
                        this.history = [];
                        ai.createDialogStates[this.componentPropertyPath] = this.history;
                    }
                    this.historyPosition = this.history.length - 1;
                    if (this.historyPosition >= 0) {
                        this.restoreStateFromMap(this.history[this.historyPosition]);
                    }
                } else {
                    // new unsaved component instances are indistinguishable, so we don't save their state
                    this.history = [];
                    this.historyPosition = -1;
                }

                if (this.isRichText) {
                    core.widgetOf(this.$response.find('textarea')).onChange(this.adjustButtonStates.bind(this));
                } else {
                    this.$response.change(this.adjustButtonStates.bind(this));
                }

                this.$el.find('#promptTextarea').mouseleave(this.adjustButtonStates.bind(this));
                this.$el.find('.generate-container').mouseenter(this.adjustButtonStates.bind(this));
                this.adjustButtonStates();

                this.$el.find('#promptTextarea').keydown((event) => {
                    if (event.which === 13 && !event.shiftKey && !event.ctrlKey && !event.altKey && !event.metaKey) {
                        this.generateButtonClicked(event);
                    }
                });
            },

            adjustButtonStates: function () {
                this.$el.find('.back-button').prop('disabled', this.historyPosition <= 0);
                this.$el.find('.forward-button').prop('disabled', this.historyPosition >= this.history.length - 1);
                this.$el.find('.generate-button').prop('disabled', !this.$prompt.val());
                let responseVal = this.getResponse();
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
                    'result': this.getResponse(),
                    'source': this.getSourceContent(),
                    'url': this.$urlField.val()
                };
            },

            saveState: function () {
                this.abortRunningCalls();
                // save if we are at the last position and the current state is different from the last saved state
                let currentState = this.makeSaveStateMap();
                let lastSavedState = this.history[this.historyPosition];
                if (!_.isEqual(currentState, lastSavedState)) {
                    console.log("saving state", currentState);
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
                this.setResponse(map['result']);
                this.setSourceContent(map['source']);
                this.adjustButtonStates();
                this.$urlField.val(map['url']);
            },

            /** Button 'Reset' was clicked. */
            resetButtonClicked: function (event) {
                event.preventDefault();
                this.abortRunningCalls();
                this.setLoading(false);
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

            contentSelectChanged: function (event) {
                console.log("contentSelectChanged", arguments);
                event.preventDefault();
                let contentSelect = this.$contentSelect.val();
                const key = this.$contentSelect.val();
                this.$urlContainer.hide();
                switch (key) {
                    case 'lastoutput':
                        this.setSourceContent(this.getResponse());
                        break;
                    case 'widget':
                        this.setSourceContent(this.widget.getValue());
                        break;
                    case 'component':
                        this.retrieveValue(this.componentPath, (value) => this.setSourceContent(value));
                        break;
                    case 'page':
                        this.retrieveValue(this.pagePath, (value) => this.setSourceContent(value));
                        break;
                    case 'empty':
                        this.setSourceContent('');
                        break;
                    case 'url':
                        this.showError();
                        this.$urlContainer.show();
                    case '-':
                        this.setSourceContent('');
                        break;
                    default:
                        if (key.startsWith('/content/')) {
                            this.retrieveValue(key, (value) => this.setSourceContent(value));
                        } else {
                            debugger;
                            this.showError('Unknown content selector value ' + key);
                        }
                }
            },

            sourceChanged: function (event) {
                event.preventDefault();
                this.$contentSelect.val('-');
            },

            setSourceContent(value) {
                if (this.isRichText) {
                    core.widgetOf(this.$sourceContent.find('textarea')).setValue(value || '');
                } else {
                    this.$sourceContent.val(value || '');
                }
            },

            getSourceContent() {
                if (this.isRichText) {
                    return core.widgetOf(this.$sourceContent.find('textarea')).getValue();
                } else {
                    return this.$sourceContent.val();
                }
            },

            retrieveValue(path, callback) {
                $.ajax({
                    url: ai.const.url.create.approximated +
                        (this.isRichText ? '.html' : '.md') + core.encodePath(path),
                    type: "GET",
                    dataType: "text",
                    success: (data) => {
                        callback(data);
                    },
                    error: (xhr, status, error) => {
                        console.error("error loading approximate markdown", xhr, status, error);
                        this.showError(status + " " + error);
                    }
                });
            },

            urlChanged(event) {
                event.preventDefault();
                const url = this.$urlField.val();
                if (url) {
                    this.showError();
                    $.ajax({
                        url: ai.const.url.create.approximated +
                            (this.isRichText ? '.html' : '.md') + '?fromurl=' + url,
                        type: "GET",
                        dataType: "text",
                        success: (data) => {
                            this.setSourceContent(data);
                        },
                        error: (xhr, status, error) => {
                            console.error("error loading approximate markdown for ", url, xhr, status, error);
                            this.showError(status + " " + error);
                        }
                    });
                }
            },

            resetHistoryButtonClicked: function (event) {
                this.history.length = 0;
                this.historyPosition = -1;
                this.resetButtonClicked(event);
                this.history.length = 0;
                this.historyPosition = -1;
            },

            backButtonClicked: function (event) {
                this.saveState();
                if (this.historyPosition > 0) {
                    this.abortRunningCalls();
                    this.setLoading(false);
                    this.historyPosition = this.historyPosition - 1;
                    let lastSavedState = this.history[this.historyPosition];
                    console.log('switching to state', this.historyPosition, this.history.length)
                    this.restoreStateFromMap(lastSavedState);
                }
            },

            forwardButtonClicked: function (event) {
                this.saveState();
                if (this.historyPosition < this.history.length - 1) {
                    this.abortRunningCalls();
                    this.setLoading(false);
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
                this.$el.find('.stop-button').prop('disabled', !loading);
            },

            generateButtonClicked: function (event) {
                event.preventDefault();
                this.setLoading(true);
                this.$response[0].scrollIntoView();

                const that = this;

                function consumeXhr(xhr) {
                    that.abortRunningCalls();
                    that.runningxhr = xhr;
                }

                let textLength = this.$textLength.val();
                let prompt = this.$prompt.val();
                let source = this.getSourceContent();

                let url = ai.const.url.general.authoring + ".create.json";
                core.ajaxPost(url, {
                        textLength: textLength,
                        inputText: source,
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
                const statusOK = data.status && data.status >= 200 && data.status < 300 && data.data && data.data.result;
                if (statusOK && data.data.result.text) {
                    console.log("Success generating text: ", data);
                    this.setLoading(false);
                    let value = data.data.result.text;
                    this.setResponse(value);
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

            setResponse: function (value) {
                if (this.isRichText) {
                    core.widgetOf(this.$response.find('textarea')).setValue(value || '');
                    this.$response.attr('data-fullresponse', value); // for debugging
                } else {
                    this.$response.val(value || '');
                }
                this.adjustButtonStates();
                if (this.$contentSelect.val() === 'lastoutput') {
                    this.$contentSelect.val('-');
                }
            },

            getResponse: function () {
                if (this.isRichText) {
                    return core.widgetOf(this.$response.find('textarea')).getValue();
                } else {
                    return this.$response.val();
                }
            },

            generateError: function (jqXHR, textStatus, errorThrown) {
                this.setLoading(false);
                console.error("Error generating text: ", arguments);
                this.showError("Error generating text: " + JSON.stringify(arguments));
            },

            showError(error) {
                if (error) {
                    this.$alert.text(error);
                    this.$alert.show();
                } else {
                    this.$alert.hide();
                }
            },

            replaceButtonClicked: function (event) {
                event.preventDefault();
                const result = this.getResponse();
                if (this.isRichText) {
                    this.widget.setValue(result);
                } else {
                    this.widget.setValue(result);
                }
                this.saveState();
                this.$el.modal('hide');
                this.widget.grabFocus();
                return false;
            },

            appendButtonClicked: function (event) {
                event.preventDefault();
                const result = this.getResponse();
                let previousValue = this.widget.getValue();
                previousValue = previousValue ? previousValue.trim() + "\n\n" : "";
                if (this.isRichText) {
                    this.widget.setValue(previousValue + "<p>" + result + "</p>");
                } else {
                    this.widget.setValue(previousValue + "\n\n" + result);
                }
                this.saveState();
                this.$el.modal('hide');
                this.widget.grabFocus();
                return false;
            },

            stopButtonClicked: function (event) {
                event.preventDefault();
                this.abortRunningCalls();
                this.setLoading(false);
                this.saveState();
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
                this.setResponse(this.streamingResult);
            },

            onStreamingError: function (eventSource, event) {
                console.log('onStreamingError', arguments);
                this.eventSource.close();
                this.abortRunningCalls();
                this.setLoading(false);
                this.showError('Connection failed.');
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
                this.showError(JSON.stringify(event.data));
            }

        });


    })(window.composum.ai, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);
// TODO(hps,10.10.23) do not store the state of the dialog persistently through closing the dialog when the component path ends with /* (literally a star)

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

        /** Maps widget paths to the saved state for the dialog. */
        ai.sidebarDialogStates = {};

        /**
         * Dialog for the AI sidebar.
         * @param options{el}
         */
        ai.SidebarDialog = Backbone.View.extend({

            initialize: function (options) {

                ai.commonDialogInit(this.$el);
                const dataholder = this.findSingleElemenet('.dataholder');
                this.componentPath = dataholder.data('componentpath');
                this.pagePath = dataholder.data('pagepath');
                this.streaming = typeof (EventSource) !== "undefined";

                this.$predefinedPrompts = this.findSingleElemenet('.predefined-prompts');
                this.$contentSelect = this.findSingleElemenet('.content-selector');
                this.$prompt = this.findSingleElemenet('.promptcontainer textarea');
                this.$alert = this.findSingleElemenet('.generalalert');
                this.$truncationalert = this.findSingleElemenet('.truncationalert');

                this.$spinner = this.findSingleElemenet('.loading-indicator');
                this.$response = this.findSingleElemenet('.ai-response-text');

                this.findSingleElemenet('.back-button').click(_.bind(this.backButtonClicked, this));
                this.findSingleElemenet('.forward-button').click(_.bind(this.forwardButtonClicked, this));
                this.findSingleElemenet('.generate-button').click(_.bind(this.generateButtonClicked, this));
                this.findSingleElemenet('.reset-button').click(_.bind(this.resetButtonClicked, this));
                this.findSingleElemenet('.reset-history-button').click(_.bind(this.resetHistoryButtonClicked, this));
                this.findSingleElemenet('.stop-button').click(_.bind(this.stopButtonClicked, this));

                this.findSingleElemenet('.predefined-prompts').change(_.bind(this.predefinedPromptsChanged, this));
                this.$prompt.change(_.bind(this.promptChanged, this));

                this.history = ai.sidebarDialogStates[this.pagePath];
                console.log('History for ', this.componentPropertyPath, ' used.'); // FIXME remove this.
                if (!this.history) {
                    this.history = [];
                    ai.sidebarDialogStates[this.pagePath] = this.history;
                }
                this.historyPosition = this.history.length - 1;
                if (this.historyPosition >= 0) {
                    this.restoreStateFromMap(this.history[this.historyPosition]);
                }

                this.$prompt.mouseleave(this.adjustButtonStates.bind(this));

                this.adjustButtonStates();
            },

            findSingleElemenet: function (selector) {
                let $el = this.$el.find(selector);
                if ($el.length !== 1) {
                    console.error('BUG! SidebarDialog: missing element for selector', selector, $el);
                }
                return $el;
            },

            setUpWidgets: function (root) {
                // CPM.widgets.setUp(root); probably not needed
            },

            adjustButtonStates: function () {
                this.findSingleElemenet('.back-button').prop('disabled', this.historyPosition <= 0);
                this.findSingleElemenet('.forward-button').prop('disabled', this.historyPosition >= this.history.length - 1);
                this.findSingleElemenet('.generate-button').prop('disabled', !this.$prompt.val()); //XXX
            },

            /** Creates a map that saves the content of all fields of this dialog. */
            makeSaveStateMap: function () {
                return {
                    'predefinedPrompts': this.$predefinedPrompts.val(),
                    'contentSelect': this.$contentSelect.val(),
                    'prompt': this.$prompt.val(), // XXX
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
                this.$prompt.val(map['prompt']); // XXX
                this.setResult(map['result']);
                this.adjustButtonStates();
            },

            resetButtonClicked: function (event) {
                event.preventDefault();
                this.saveState();
                this.restoreStateFromMap({});
                return false;
            },

            resetHistoryButtonClicked: function (event) {
                this.history = [];
                this.historyPosition = -1;
                this.resetButtonClicked(event);
            },

            predefinedPromptsChanged: function (event) {
                event.preventDefault();
                let predefinedPrompt = this.$predefinedPrompts.val();
                this.$prompt.val(predefinedPrompt); // XXX
                this.adjustButtonStates();
                return false;
            },

            promptChanged: function (event) { // XXX
                event.preventDefault();
                this.$predefinedPrompts.val(this.$predefinedPrompts.find('option:first').val());
                this.adjustButtonStates();
                return false;
            },

            stopButtonClicked: function (event) {
                this.abortRunningCalls();
                this.setLoading(false);
                this.adjustButtonStates();
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
                    this.$spinner.show(); // XXX
                    this.$alert.hide();
                    this.$alert.text('');
                } else {
                    this.$spinner.hide();
                }
            },

            generateButtonClicked: function (event) { // XXX
                event.preventDefault();
                this.setLoading(true);

                const that = this;

                function consumeXhr(xhr) {
                    that.abortRunningCalls();
                    that.runningxhr = xhr;
                }

                let contentSelect = this.$contentSelect.val();
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
                        inputText: inputText,
                        inputPath: inputPath,
                        streaming: this.streaming,
                        textLength: "0|",
                        prompt: prompt
                    }, {dataType: 'json', xhrconsumer: consumeXhr},
                    _.bind(this.generateSuccess, this), _.bind(this.generateError, this));
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

            generateSuccess: function (data) { // XXX
                this.setLoading(false);
                const statusOK = data.status && data.status >= 200 && data.status < 300 && data.data && data.data.result;
                if (statusOK && data.data.result.text) {
                    console.log("Success generating text: ", data);
                    let value = data.data.result.text;
                    setValue(value);
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

            setResult: function (value) { // XXX
                this.$response.text(value || '');
                this.adjustButtonStates();
            },

            getResult: function () {
                return this.$response.text();
            },

            generateError: function (jqXHR, textStatus, errorThrown) {
                this.setLoading(false);
                console.error("Error generating text: ", arguments);
                this.$alert.html("Error generating text: " + JSON.stringify(arguments));
                this.$alert.show();
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
                console.log('Complete text: ', this.streamingResult);
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

        /**
         * register this tool as a pages context tool for initialization after load of the context tools set
         */
        pages.contextTools.addTool(function (contextTabs) {
            var tool = core.getWidget(contextTabs.el, '.composum-pages-options-ai-tools-sidebar', ai.SidebarDialog);
            if (tool) {
                tool.contextTabs = contextTabs;
            }
            return tool;
        });

    })(window.composum.ai, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

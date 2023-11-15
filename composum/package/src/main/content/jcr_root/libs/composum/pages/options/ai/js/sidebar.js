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

        /** Maps widget paths to the last scrollTop for the dialog. */
        ai.scrollPositions = {};

        /**
         * Dialog for the AI sidebar.
         * Since we'd like to allow a chat, we always create a new promptcontainer after a response. That means however
         * that we have a moving target for output. To handle that:
         * - when the user changes a promptcontainer textarea, we remove the following promptcontainers.
         * - when the user clicks on 'submit', we submit until the last existing promptcontainer.
         *
         * @param options{el}
         */
        ai.SidebarDialog = Backbone.View.extend({

            initialize: function (options) {
                try {
                    let that = this;
                    ai.commonDialogInit(this.$el);
                    const dataholder = this.findSingleElemenet('.dataholder');
                    this.componentPath = dataholder.data('componentpath');
                    this.pagePath = dataholder.data('pagepath');
                    this.streaming = typeof (EventSource) !== "undefined";

                    this.$predefinedPrompts = this.findSingleElemenet('.predefined-prompts');
                    this.$contentSelect = this.findSingleElemenet('.content-selector');
                    this.$alert = this.findSingleElemenet('.generalalert');
                    this.$truncationalert = this.findSingleElemenet('.truncationalert');
                    this.$prompt = this.findSingleElemenet('.promptcontainer.first textarea');
                    this.$response = this.$el.find('.ai-response-text').first();
                    this.$scrollHandle = this.findSingleElemenet('.composum-pages-tools_panel');

                    this.$el.on('change', '.promptcontainer textarea', this.promptChanged.bind(this));
                    this.$el.on('mouseleave', '.promptcontainer textarea', this.adjustButtonStates.bind(this));

                    this.findSingleElemenet('.back-button').click(this.backButtonClicked.bind(this));
                    this.findSingleElemenet('.forward-button').click(this.forwardButtonClicked.bind(this));
                    this.findSingleElemenet('.generate-button').click(this.generateButtonClicked.bind(this));
                    this.$el.on('keypress', '.promptcontainer textarea', (e) => {
                        if (e.which === 13 && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
                            this.promptChanged(e);
                            // bind 'Control-Enter' or 'Command-Enter' in the textarea to the generate button
                            this.generateButtonClicked(e);
                        }
                    });
                    this.findSingleElemenet('.reset-button').click(this.resetButtonClicked.bind(this));
                    this.findSingleElemenet('.reset-history-button').click(this.resetHistoryButtonClicked.bind(this));
                    this.findSingleElemenet('.stop-button').click(this.stopButtonClicked.bind(this));

                    this.findSingleElemenet('.predefined-prompts').change(this.predefinedPromptsChanged.bind(this));

                    this.initialState = this.makeSaveStateMap();

                    this.history = ai.sidebarDialogStates[this.pagePath];
                    console.log('History for ', this.pagePath, ' used.');
                    if (!this.history) {
                        this.history = [];
                        ai.sidebarDialogStates[this.pagePath] = this.history;
                    }
                    this.historyPosition = this.history.length - 1;
                    if (this.historyPosition >= 0) {
                        this.restoreStateFromMap(this.history[this.historyPosition]);
                    } else {
                        this.adjustChatCount(0);
                    }

                    this.adjustButtonStates();

                    const scrollTop = ai.scrollPositions[this.pagePath];
                    if (scrollTop) {
                        setTimeout(() => this.$scrollHandle.scrollTop(scrollTop), 0);
                    }
                    this.$scrollHandle.scroll(() => ai.scrollPositions[this.pagePath] = this.$scrollHandle.scrollTop());
                } catch (e) {
                    console.error(e);
                    // can't do anything about that, but at least don't break other scripts
                }
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
                // .generate-button is enabled if any of the prompts has content
                this.findSingleElemenet('.generate-button').prop('disabled',
                    !this.$el.find('.promptcontainer textarea').toArray().some(function (el) {
                        return el.value.trim().length > 0;
                    }));
            },

            saveState: function () {
                this.abortRunningCalls();
                // save if we are at the last position and the current state is different from the last saved state
                let currentState = this.makeSaveStateMap();
                let lastSavedState = this.history[this.historyPosition];
                if (!_.isEqual(currentState, lastSavedState)) {
                    console.log("SAVING STATE!", currentState);
                    this.history.push(currentState);
                    this.historyPosition = this.history.length - 1;
                }
                console.log('saveState', this.historyPosition, this.history.length, this.history);
                this.adjustButtonStates();
            },

            /** Creates a map that saves the content of all fields of this dialog. */
            makeSaveStateMap: function () {
                return {
                    'predefinedPrompts': this.$predefinedPrompts.val(),
                    'contentSelect': this.$contentSelect.val(),
                    'firstprompt': this.findSingleElemenet('.promptcontainer.first textarea').val(),
                    'chat': this.getChat(),
                    'result': this.getResult()
                }
            },

            /** Restores the state of this dialog from the given map. */
            restoreStateFromMap: function (map) {
                this.$predefinedPrompts.val(map['predefinedPrompts']);
                this.$contentSelect.val(map['contentSelect']);
                this.findSingleElemenet('.promptcontainer.first textarea').val(map['firstprompt']);
                this.findSingleElemenet('.first .ai-response-text').text(map['firstresponse'] || '');
                let chatCount = (map['chat'] || []).filter(function (chatitem) {
                    return chatitem.role === 'user';
                }).length;
                this.adjustChatCount(0);
                this.adjustChatCount(chatCount);
                if (map.chat && map.chat.length > 0) this.findSingleElemenet('.first .ai-response-text').text(map.chat[0].content || '');
                let chatFields = this.$el.find('.chat');
                for (let i = 1; map.chat && i < map.chat.length; i++) {
                    let chatitem = map['chat'][i];
                    let chatField = chatFields.eq(i - 1);
                    if (chatitem.role === 'user') {
                        chatField.find('textarea').val(chatitem.content);
                    } else {
                        chatField.find('.ai-response-text').text(chatitem.content || '');
                    }
                }
                this.adjustButtonStates();
            },

            resetButtonClicked: function (event) {
                event.preventDefault();
                this.saveState();
                this.restoreStateFromMap(this.initialState);
                return false;
            },

            resetHistoryButtonClicked: function (event) {
                this.history.length = 0;
                this.historyPosition = -1;
                this.resetButtonClicked(event);
                this.history.length = 0;
                this.historyPosition = -1;
            },

            /** We delete all input- and their output fields if the input field is empty, starting from the end. */
            deleteEmptyChatFields: function () {
                while (this.$el.find('.promptcontainer.chat textarea').last().length
                && !this.$el.find('.promptcontainer.chat textarea').last().val()) {
                    this.$el.find('.promptcontainer.chat').last().remove();
                    this.$el.find('.ai-response.chat').last().remove();
                }

                this.$response = this.$el.find('.ai-response-text').last();
            },

            /** If numberOfChats is smaller than the current number of chat fields, remove those.
             * If it's larger, add by copying .airesponse.first and .promptcontainer.template and clearing the texts. */
            adjustChatCount: function (numberOfChats) {
                console.log('adjustChatCount', numberOfChats);
                let numberOfExistingChats = this.getChatCount();
                if (numberOfExistingChats < numberOfChats) {
                    let $promptTemplate = this.$el.find('.promptcontainertemplate');
                    let $answerTemplate = this.$el.find('.ai-response.first');
                    for (let i = numberOfExistingChats; i < numberOfChats; i++) {
                        let $newChat = $promptTemplate.clone();
                        $newChat.removeClass('promptcontainertemplate');
                        $newChat.removeClass('hidden');
                        $newChat.addClass('promptcontainer');
                        $newChat.addClass('chat');
                        this.$scrollHandle.append($newChat);

                        let $newAnswer = $answerTemplate.clone();
                        $newAnswer.removeClass('first');
                        $newAnswer.addClass('chat');
                        $newAnswer.find('.ai-response-text').text('');
                        this.$scrollHandle.append($newAnswer);
                    }
                } else if (numberOfExistingChats > numberOfChats) {
                    this.$el.find('.promptcontainer.chat').slice(numberOfChats).remove();
                    this.$el.find('.ai-response.chat').slice(numberOfChats).remove();
                }

                this.$response = this.$el.find('.ai-response-text').last();
            },

            getChatCount: function () {
                return this.$el.find('.promptcontainer.chat').length;
            },

            predefinedPromptsChanged: function (event) {
                event.preventDefault();
                let predefinedPrompt = this.$predefinedPrompts.val();
                this.adjustChatCount(0);
                this.$prompt.val(predefinedPrompt);
                this.adjustButtonStates();
                this.lastChangedPrompt = event.target;
                return false;
            },

            promptChanged: function (event) {
                console.log('promptChanged', event);
                this.$predefinedPrompts.val(this.$predefinedPrompts.find('option:first').val());
                this.adjustButtonStates();
                // delete all prompts after the modified prompt, but only if the prompt was actually modified.
                // for that we save the state before the change and compare it to the state after the change.
                // the value is always saved in the attribute data-previous-value on the event target (the text area).
                const previousValue = $(event.target).data('previous-value');
                const currentValue = $(event.target).val();
                $(event.target).data('previous-value', currentValue);
                if (!previousValue || previousValue !== currentValue) {
                    let $changedField = $(event.target).closest('.promptcontainer');
                    if ($changedField.hasClass('first')) {
                        this.adjustChatCount(0);
                        this.$el.find('.ai-response.first .ai-response-text').text('');
                    } else {
                        let $chatFields = this.$el.find('.promptcontainer.chat');
                        let index = $chatFields.index($changedField);
                        this.adjustChatCount(index + 1);
                        this.$el.find('.ai-response.chat').last().find('.ai-response-text').text('');
                    }
                }
                this.lastChangedPrompt = event.target;
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
                    console.log('switching to state', this.historyPosition, this.history.length, lastSavedState)
                    this.restoreStateFromMap(lastSavedState);
                }
            },

            forwardButtonClicked: function (event) {
                this.saveState();
                if (this.historyPosition < this.history.length - 1) {
                    this.historyPosition = this.historyPosition + 1;
                    let lastSavedState = this.history[this.historyPosition];
                    console.log('switching to state', this.historyPosition, this.history.length, lastSavedState)
                    this.restoreStateFromMap(lastSavedState);
                }
            },

            setLoading: function (loading) {
                if (loading) {
                    this.$el.find('.loading-indicator').last().show();
                    this.$alert.hide();
                    this.$alert.text('');
                } else {
                    this.$el.find('.loading-indicator').hide();
                    if (this.$response.text()) {
                        this.adjustChatCount(this.getChatCount() + 1);
                    }
                }
                this.$el.find('.stop-button').prop('disabled', !loading);
            },

            /** Format  [{"role":"assistant","content":"Answer 1"},{"role":"user","content":"Another question"}] for AIServlet chat parameter.
             * Contains the first response (as that fits the conversation model) the other .chat items. */
            getChat: function () {
                let chat = [];
                let firstResponse = this.$el.find('.ai-response.first .ai-response-text').text();
                if (firstResponse) {
                    chat.push({"role": "assistant", "content": firstResponse});
                }
                this.$el.find('.chat').each(function (index, element) {
                    let $element = $(element);
                    if ($element.hasClass('promptcontainer')) {
                        let value = $element.find('textarea').val();
                        chat.push({"role": "user", "content": value});
                    } else {
                        let responseText = $element.find('.ai-response-text').text();
                        chat.push({"role": "assistant", "content": responseText});
                    }
                });
                return chat;
            },

            /* delay generation a bit so that promptChanged can be called first */
            generateButtonClicked: function (event) {
                console.log('generateButtonClicked', event);
                event.preventDefault();
                if (this.lastChangedPrompt) {
                    $(this.lastChangedPrompt).trigger('change');
                }
                let that = this;
                setTimeout(() => that.generate(), 200);
            },

            generate: function () {
                console.log('generate');
                this.setLoading(true);
                this.deleteEmptyChatFields();

                const that = this;

                function consumeXhr(xhr) {
                    that.abortRunningCalls();
                    that.runningxhr = xhr;
                }

                let contentSelect = this.$contentSelect.val();
                let chat = this.getChat();
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
                        prompt: this.$prompt.val(),
                        chat: JSON.stringify(chat)
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
                this.$el.find('.loading-indicator').hide();
            },

            generateSuccess: function (data) {
                const statusOK = data.status && data.status >= 200 && data.status < 300 && data.data && data.data.result;
                if (statusOK && data.data.result.text) {
                    console.log("Success generating text: ", data);
                    let value = data.data.result.text;
                    this.setResult(value);
                    this.setLoading(false);
                    this.saveState();
                    this.scrollToLastPromptContainer();
                } else if (statusOK && data.data.result.streamid) {
                    const streamid = data.data.result.streamid;
                    this.startStreaming(streamid);
                } else {
                    console.error("Error generating text: ", data);
                    this.$alert.html("Error generating text: " + JSON.stringify(data));
                    this.$alert.show();
                    this.setLoading(false);
                }
            },

            /** Scrolls the last promptcontainer to the top within this.$scrollHandle so that the response field is visible. */
            scrollToLastPromptContainer: function () {
                let lastPromptContainer = this.$el.find('.promptcontainer').last();
                if (lastPromptContainer.length) {
                    setTimeout(() => {
                        this.$scrollHandle.animate({scrollTop: lastPromptContainer.position().top + this.$scrollHandle.scrollTop()}, 'slow');
                    }, 500);
                }
            },

            setResult: function (value) {
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
                this.scrollToLastPromptContainer();
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
                this.scrollToLastPromptContainer(); // before setloading since otherwise wrong position
                this.setLoading(false);
                this.saveState();
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

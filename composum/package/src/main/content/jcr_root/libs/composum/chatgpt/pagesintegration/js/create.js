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
        chatgpt.const.url.create = {
            createDialog: '/bin/cpm/platform/chatgpt/dialog.creationDialog.html'
        }

        chatgpt.openCreationDialog = function (event) {
            let $target = $(event.target);
            let path = $target.data('path');
            let pagePath = $target.data('pagepath');
            let property = $target.data('property');
            let propertypath = $target.data('propertypath');
            let outputfield = chatgpt.searchInput($target);
            let widget = core.widgetOf(outputfield);
            let isRichText = widget && !!widget.richText;
            let url = chatgpt.const.url.create.createDialog + core.encodePath(path + '/' + property) +
                "?propertypath=" + encodeURIComponent(propertypath) + "&pages.locale=" + pages.getLocale() + "&richtext=" + isRichText;
            core.openFormDialog(url, chatgpt.CreateDialog, {
                widget: widget, isRichText: isRichText,
                componentPath: path, pagePath: pagePath, componentPropertyPath: path + '/' + property
            });
        }

        /** Maps widget paths to the saved state for the dialog. */
        chatgpt.createDialogStates = {};

        /**
         * Dialog for categorize - giving a page categories.
         * The suggested categories are loaded via an additional HTML AJAX request that loads the suggested categories.
         * @param options{widget, isRichText, componentPath, pagePath, componentPropertyPath}
         */
        chatgpt.CreateDialog = core.components.FormDialog.extend({

            initialize: function (options) {
                core.components.FormDialog.prototype.initialize.apply(this, [options]);
                chatgpt.commonDialogInit(this.$el);
                this.widget = options.widget;
                this.isRichText = options.isRichText;
                this.componentPath = options.componentPath;
                this.pagePath = options.pagePath;
                this.componentPropertyPath = options.componentPropertyPath;

                this.$predefinedPrompts = this.$el.find('.predefined-prompts');
                this.$contentSelect = this.$el.find('.content-selector');
                this.$textLength = this.$el.find('.text-length-selector');
                this.$prompt = this.$el.find('.prompt-textarea');
                this.$outputField = this.$el.find('.chatgpt-response-field');
                this.$alert = this.$el.find('.alert');
                this.$loading = this.$el.find('.loading-indicator');
                this.$response = this.$el.find('.chatgpt-response-field');

                this.$el.find('.back-button').click(_.bind(this.backButtonClicked, this));
                this.$el.find('.forward-button').click(_.bind(this.forwardButtonClicked, this));
                this.$el.find('.generate-button').click(_.bind(this.generateButtonClicked, this));
                this.$el.find('.reset-button').click(_.bind(this.resetButtonClicked, this));

                this.$el.find('.predefined-prompts').change(_.bind(this.predefinedPromptsChanged, this));
                this.$prompt.change(_.bind(this.promptChanged, this));

                this.$el.find('.replace-button').click(_.bind(this.replaceButtonClicked, this));
                this.$el.find('.append-button').click(_.bind(this.appendButtonClicked, this));
                this.$el.find('.cancel-button').click(_.bind(this.cancelButtonClicked, this));
                this.$el.on('hidden.bs.modal', _.bind(this.abortRunningCalls, this));

                if (!this.widget) {
                    console.log('No widget found for ', this.componentPropertyPath);
                    this.$alert.show();
                    this.$alert.text('Bug, please report: no widget found for ' + this.componentPropertyPath);
                }

                this.history = chatgpt.createDialogStates[this.componentPropertyPath];
                if (!this.history) {
                    this.history = [];
                    chatgpt.createDialogStates[this.componentPropertyPath] = this.history;
                }
                this.historyPosition = this.history.length - 1;
                if (this.historyPosition >= 0) {
                    this.restoreStateFromMap(this.history[this.historyPosition]);
                }
            },

            /** Creates a map that saves the content of all fields of this dialog. */
            makeSaveStateMap: function () {
                return {
                    'predefinedPrompts': this.$predefinedPrompts.val(),
                    'contentSelect': this.$contentSelect.val(),
                    'textLength': this.$textLength.val(),
                    'prompt': this.$prompt.val(),
                    'outputField': this.$outputField.val()
                };
            },

            saveState: function () {
                // save if we are at the last position and the current state is different from the last saved state
                let currentState = this.makeSaveStateMap();
                let lastSavedState = this.history[this.historyPosition];
                if (!_.isEqual(currentState, lastSavedState)) {
                    this.history.push(currentState);
                    this.historyPosition = this.history.length - 1;
                }
            },

            /** Restores the state of this dialog from the given map. */
            restoreStateFromMap: function (map) {
                this.$predefinedPrompts.val(map['predefinedPrompts']);
                this.$contentSelect.val(map['contentSelect']);
                this.$textLength.val(map['textLength']);
                this.$prompt.val(map['prompt']);
                this.$outputField.val(map['outputField']);
            },

            /** Button 'Reset' was clicked. */
            resetButtonClicked: function (event) {
                event.preventDefault();
                this.restoreStateFromMap({});
                return false;
            },

            predefinedPromptsChanged: function (event) {
                event.preventDefault();
                let predefinedPrompt = this.$predefinedPrompts.val();
                this.$prompt.val(predefinedPrompt);
                return false;
            },

            promptChanged: function (event) {
                event.preventDefault();
                this.$predefinedPrompts.val(this.$predefinedPrompts.find('option:first').val());
                return false;
            },

            backButtonClicked: function (event) {
                if (this.historyPosition == this.history.length - 1) {
                    this.saveState();
                }
                if (this.historyPosition > 0) {
                    this.historyPosition = this.historyPosition - 1;
                    this.restoreStateFromMap(this.history[this.historyPosition]);
                }
            },

            forwardButtonClicked: function (event) {
                if (this.historyPosition < this.history.length - 1) {
                    this.historyPosition = this.historyPosition + 1;
                    this.restoreStateFromMap(this.history[this.historyPosition]);
                }
            },

            setLoading: function (loading) {
                if (loading) {
                    this.$loading.show();
                } else {
                    this.$loading.hide();
                }
                this.$alert.hide();
            },

            generateButtonClicked: function (event) {
                event.preventDefault();
                this.setLoading(true);
                if (this.$outputField.val()) {
                    this.saveState();
                }

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
                    inputText = this.$outputField.val();
                }

                let url = chatgpt.const.url.general.authoring + ".create.json";
                core.ajaxPost(url, {
                        contentSelect: contentSelect,
                        textLength: textLength,
                        inputText: inputText,
                        inputPath: inputPath,
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
            },

            generateSuccess: function (data) {
                this.setLoading(false);
                if (data.status >= 200) {
                    console.log("Success generating text: ", data);
                    let value = data.data.result.text;
                    if (this.isRichText) {
                        core.widgetOf(this.$response.find('textarea')).setValue(value);
                    } else {
                        this.$response.val(value);
                    }
                } else {
                    console.error("Error generating text: ", data);
                    this.$alert.html("Error generating text: " + JSON.stringify(data));
                    this.$alert.show();
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
                if (this.isRichText) {
                    let suggestion = core.widgetOf(this.$response.find('textarea')).getValue();
                    this.widget.setValue(suggestion);
                } else {
                    this.widget.setValue(this.$response.val());
                }
                this.$el.modal('hide');
                this.widget.grabFocus();
                return false;
            },

            appendButtonClicked: function (event) {
                event.preventDefault();
                let previousValue = this.widget.getValue();
                previousValue = previousValue ? previousValue.trim() + "\n\n" : "";
                if (this.isRichText) {
                    this.widget.setValue(previousValue + "<p>" + this.$response.val() + "</p>"); // HTML?
                } else {
                    this.widget.setValue(previousValue + "\n" + this.$response.val());
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
            }

        });


    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

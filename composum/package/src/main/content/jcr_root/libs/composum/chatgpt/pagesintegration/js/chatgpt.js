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
                translationDialog: '/bin/cpm/platform/chatgpt/dialog.translationDialog.html',
                categorizeDialog: '/bin/cpm/platform/chatgpt/dialog.categorizeDialog.html',
                categorizeSuggestions: '/bin/cpm/platform/chatgpt/dialog.categorizeDialog.suggestions.html',
                createDialog: '/bin/cpm/platform/chatgpt/dialog.creationDialog.html'
            }
        };

        /** Will be called from Pages after a dialog is rendered via the dialogplugins hook.
         * Thus we bind ourselves into our buttons rendered into the dialog by the labelextension.jsp . */
        chatgpt.dialogInitializeView = function (dialog, $element) {
            console.log('chatgpt.dialogInitializeView', dialog, $element);
            let $translationButtons = $element.find('.widget-chatgptaction.action-translate');
            $translationButtons.click(chatgpt.openTranslateDialog);
            let $categorizeButtons = $element.find('.widget-chatgptaction.action-pagecategories');
            $categorizeButtons.click(chatgpt.openCategorizeDialog);
            let $createButtons = $element.find('.widget-chatgptaction.action-create');
            $createButtons.click(chatgpt.openCreationDialog);
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
                let widget = core.widgetOf(this.$outputfield);
                if (widget) {
                    if (widget.richText) {
                        widget.setValue(this.$translation.html());
                    } else {
                        widget.setValue(this.$translation.text());
                    }
                    widget.grabFocus();
                } else {
                    console.error("Bug: cannot find widget for ", this.$outputfield);
                }
                this.$el.modal('hide');
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

        /** Opens the categorize dialog. The current categories are not taken from the resource, but from the dialog
         * this is called from, since the user might have modified this. */
        chatgpt.openCategorizeDialog = function (event) {
            console.log('openCategorizeDialog', arguments);
            let $target = $(event.target);
            var path = $target.data('path');
            var property = $target.data('property');
            let $widget = $(event.target).closest('div.form-group');
            let $inputs = $widget.find('input[type="text"][name="category"]');
            // make an array 'categories' of the values of all inputs with name 'category'
            let categories = [];
            $inputs.each(function () {
                let value = $(this).val().trim();
                if (value) {
                    categories.push(value);
                }
            });
            var url = chatgpt.const.url.categorizeDialog + core.encodePath(path + '/' + property);
            var urlparams = '';
            if (categories.length > 0) {
                urlparams += "?category=" + categories.map(encodeURIComponent).join("&category=");
            }
            url = url + urlparams;
            core.openFormDialog(url, chatgpt.CategorizeDialog, {
                widget: $widget,
                categories: categories,
                path: path,
                property: property,
                categoryparams: urlparams
            });
        }

        /**
         * Dialog for categorize - giving a page categories.
         * The suggested categories are loaded via an additional HTML AJAX request that loads the suggested categories.
         * @param options{widget, categories, path, property, categoryparams}
         */
        chatgpt.CategorizeDialog = core.components.FormDialog.extend({

            /** $el is the dialog */
            initialize: function (options) {
                core.components.FormDialog.prototype.initialize.apply(this, [options]);
                this.widget = options.widget;
                this.categories = options.categories;
                this.path = options.path;
                this.property = options.property;
                this.categoryparams = options.categoryparams;
                this.$suggestions = this.$el.find('div.suggestions');
                this.loadSuggestions();
                // bind button cancel is not necessary - it is already bound to close by bootstrap
                this.$el.find('button.accept').click(_.bind(this.accept, this));
                this.$el.find('input[type="checkbox"]').change(_.bind(this.duplicateChanges, this));
            },

            /** Load the suggestions for categories. */
            loadSuggestions: function () {
                var url = chatgpt.const.url.categorizeSuggestions +
                    core.encodePath(this.path + '/' + this.property + this.categoryparams);
                core.getHtml(url, _.bind(this.onSuggestions, this));
            },

            onSuggestions: function (data) {
                this.$suggestions.html(data);
                this.$suggestions.find('input[type="checkbox"]').change(_.bind(this.duplicateChanges, this));
                this.$el.find('.current-categories input[type="checkbox"]').each(_.bind(function (index, element) {
                    this.duplicateChanges({target: element});
                }, this));
                this.$el.find('.loading-curtain').hide();
            },

            /** When a checkbox is changed we look for a second checkbox with the same value and synchronize it's state. */
            duplicateChanges: function (event) {
                let checkbox = event.target;
                let value = checkbox.value;
                let checked = checkbox.checked;
                this.$el.find('input[type="checkbox"][value="' + value + '"]').each(function () {
                    this.checked = checked;
                });
            },

            /** Button 'Accept' was clicked. */
            accept: function (event) {
                // collect the categories from the checked inputs
                let categories = [];
                this.$el.find('input[type="checkbox"]:checked').each(function () {
                    let value = $(this).val();
                    // remove a <p></p> around the value if it is there. Artifact of our HTML rendering.
                    if (value.startsWith('<p>') && value.endsWith('</p>')) {
                        value = value.substring(3, value.length - 4);
                    }
                    if (!categories.includes(value)) {
                        categories.push(value);
                    }
                });
                this.saveCategories(categories);
            },

            saveCategories: function (categories) {
                console.log('saveCategories', categories);
                let categoryWidget = core.widgetOf(this.widget);
                categoryWidget.setValue(categories);
            }

        });

        chatgpt.openCreationDialog = function (event) {
            let $target = $(event.target);
            let path = $target.data('path');
            let pagePath = $target.data('pagepath');
            let property = $target.data('property');
            let propertypath = $target.data('propertypath');
            let url = chatgpt.const.url.createDialog + core.encodePath(path + '/' + property) +
                "?propertypath=" + encodeURIComponent(propertypath) + "&pages.locale=" + pages.getLocale();
            core.openFormDialog(url, chatgpt.CreateDialog, {
                outputfield: chatgpt.searchInput($target),
                componentPath: path, pagePath: pagePath
            });
        }

        /**
         * Dialog for categorize - giving a page categories.
         * The suggested categories are loaded via an additional HTML AJAX request that loads the suggested categories.
         * @param options{outputfield, componentPath, pagePath}
         */
        chatgpt.CreateDialog = core.components.FormDialog.extend({

            initialize: function (options) {
                core.components.FormDialog.prototype.initialize.apply(this, [options]);
                this.$outputfield = options.outputfield;
                this.widget = core.widgetOf(this.$outputfield);
                this.componentPath = options.componentPath;
                this.pagePath = options.pagePath;

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

                this.$el.find('.predefined-prompts').change(_.bind(this.predefinedPromptsChanged, this));
                this.$prompt.change(_.bind(this.promptChanged, this));

                // bind buttons replace-button and append-button
                this.$el.find('.replace-button').click(_.bind(this.replaceButtonClicked, this));
                this.$el.find('.append-button').click(_.bind(this.appendButtonClicked, this));

                if (!this.widget) {
                    console.log('No widget found for ', this.$outputfield);
                    this.$alert.show();
                    this.$alert.text('Bug, please report: no widget found for ' + this.$outputfield);
                }
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
                alert('Back button clicked - not implemented yet');
            },

            forwardButtonClicked: function (event) {
                alert('Forward button clicked - not implemented yet');
            },

            setLoading(loading) {
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

                let predefinedPrompt = this.$predefinedPrompts.val();
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

                let url = chatgpt.const.url.authoring + ".create.json";
                // FIXME(hps,16.05.23) implement aborting of the request
                core.ajaxPost(url, {
                    contentSelect: contentSelect,
                    textLength: textLength,
                    inputText: inputText,
                    inputPath: inputPath,
                    prompt: prompt
                }, {dataType: 'json'}, _.bind(this.generateSuccess, this), _.bind(this.generateError, this));
                return false;
            },

            generateSuccess: function (data) {
                this.setLoading(false);
                if (data.status >= 200) {
                    console.log("Success generating text: ", data);
                    this.$response.val(data.data.result.text);
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
                let widget = this.widget;
                if (widget) {
                    if (widget.richText) {
                        widget.setValue(this.$response.html());
                    } else {
                        widget.setValue(this.$response.text());
                    }
                    widget.grabFocus();
                } else {
                    console.error("Bug: cannot find widget for ", this.$outputfield);
                }
                this.$el.modal('hide');
                return false;
            },

            appendButtonClicked: function (event) {
                event.preventDefault();
                let widget = core.widgetOf(this.$outputfield);
                let previousValue = widget.getValue();
                previousValue = previousValue ? previousValue.trim() + "\n\n" : "";
                if (widget) {
                    if (widget.richText) {
                        widget.setValue(previousValue + "<p>" + this.$response.html() + "</p>");
                    } else {
                        widget.setValue(previousValue + this.$response.text());
                    }
                    widget.grabFocus();
                } else {
                    console.error("Bug: cannot find widget for ", this.$outputfield);
                }
                this.$el.modal('hide');
                return false;
            }

        });


    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

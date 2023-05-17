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
        chatgpt.const.url.categorize = {
            categorizeDialog: '/bin/cpm/platform/chatgpt/dialog.categorizeDialog.html',
            categorizeSuggestions: '/bin/cpm/platform/chatgpt/dialog.categorizeDialog.suggestions.html',
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
            var url = chatgpt.const.url.categorize.categorizeDialog + core.encodePath(path + '/' + property);
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
                var url = chatgpt.const.url.categorize.categorizeSuggestions +
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

    })(window.composum.chatgpt, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

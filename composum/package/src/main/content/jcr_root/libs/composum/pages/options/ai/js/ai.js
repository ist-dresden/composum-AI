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
    window.composum.ai = window.composum.ai || {};

    if (window.composum.pages.dialogs.const.dialogplugins.indexOf(window.composum.ai) < 0) {
        window.composum.pages.dialogs.const.dialogplugins.push(window.composum.ai);
    }

    (function (ai, dialogs, pages, core, components) {
        'use strict';

        ai.const = ai.const || {};
        ai.const.url = ai.const.url || {};
        // all dialogs create their own subnodes in ai.const.url to put that into several files.
        ai.const.url.general = {
            authoring: '/bin/cpm/ai/authoring',
            markdown: '/bin/cpm/ai/approximated',
            config: '/bin/cpm/ai/config'
        };

        /** Will be called from Pages after a dialog is rendered via the dialogplugins hook.
         * Thus we bind ourselves into our buttons rendered into the dialog by the labelextension.jsp */
        ai.dialogInitializeView = function (dialog, $element) {
            // console.log('ai.dialogInitializeView', dialog, $element);
                let $translationButtons = $element.find('.widget-ai-action.action-translate');
                $translationButtons.click(ai.openTranslateDialog);
                let $categorizeButtons = $element.find('.widget-ai-action.action-pagecategories');
                $categorizeButtons.click(ai.openCategorizeDialog);
                let $createButtons = $element.find('.widget-ai-action.action-create');
                $createButtons.click(ai.openCreationDialog);
        };

        /** Looks for the actual text input or textarea that belongs to the labelextension. */
        ai.searchInput = function ($labelextension) {
            var $formgroup = $labelextension.closest('div.form-group');
            var $input = $formgroup.find('input[type="text"],textarea');
            if ($input.length >= 1 && $input.length <= 2) {
                // sanity check; for code area there are actually 2 outputs
                return $input;
            }
            console.error('BUG! searchInput: no input found', $labelextension);
        };

        /** Common initialization called from all dialogs on opening. */
        ai.commonDialogInit = function ($el) {
            $el.find("button.maximize-button").click(ai.maximizeRestoreFunc($el, true));
            $el.find("button.restore-button").click(ai.maximizeRestoreFunc($el, false));
            $el.find("button.help-button").click(ai.openHelpDialog.bind(this));
            ai.addDragging($el);
            // Fix that ensures scrolling works for the dialog we've been called on.
            $el.on("hidden.bs.modal", function (e) {
                if ($('.modal:visible').length) {
                    $('body').addClass('modal-open');
                }
            });
        };

        ai.maximizeRestoreFunc = function ($el, ismaximize) {
            let origcss;
            return function (event) {
                event.preventDefault();
                $el.toggleClass("dialog-size-maximized");
                // save original position and size of dialog which could have been modified by dragging
                const $dialog = $el.find(".modal-dialog");
                if (ismaximize) {
                    origcss = $dialog.css(["top", "left", "position"]);
                    $dialog.css("position", "");
                    $dialog.css("left", "");
                    $dialog.css("top", "");
                } else {
                    $dialog.css(origcss);
                }
                return false;
            };
        };

        /** Creates a printable text from various error variants. */
        ai.errorText = function (error) {
            var text;
            if (typeof error === 'string') {
                text = error;
            } else if (error.data && typeof error.data === 'string') {
                text = error.data;
            } else if (error.data) {
                text = JSON.stringify(error.data);
            } else {
                text = error instanceof Error ? error.message : JSON.stringify(error);
            }
            return text;
        };

        ai.openHelpDialog = function (event) {
            event.preventDefault();
            let $button = $(event.target);
            let url = $button.data('helpurl');
            core.openFormDialog(url, ai.HelpDialog);
            return false;
        };

        /** A dialog showing help. */
        ai.HelpDialog = components.LoadedDialog.extend({

            initialize: function (options) {
                components.LoadedDialog.prototype.initialize.call(this, options);
                ai.commonDialogInit(this.$el);
            }

        });

        /** Adds dragging to the modal header. */
        ai.addDragging = function ($el) {
            let dragging = false;
            let offset = {};
            let marginTop = 0;
            const $dialog = $el.find(".modal-dialog");
            const $header = $dialog.find(".modal-header");

            function handleMouseDown(e) {
                if ($(e.target).closest("button").length > 0 || e.which !== 1) {
                    return;
                }

                e.preventDefault();
                console.log("start dragging");
                if ($dialog.css('marginTop')) {
                    marginTop = parseInt($dialog.css('marginTop'), 10);
                }

                $dialog.css({
                    position: 'absolute',
                    left: $dialog.offset().left,
                    top: $dialog.offset().top - marginTop
                });

                dragging = true;
                offset = {
                    x: e.clientX - $dialog.offset().left,
                    y: e.clientY - $dialog.offset().top - marginTop
                };

                $(document).on("mousemove", handleMouseMove);
                $(document).on("mouseup", handleMouseUp);
            }

            function handleMouseMove(e) {
                if (dragging) {
                    e.preventDefault();
                    console.log("continue dragging: " + e.clientX + ", " + e.clientY);
                    $dialog.offset({
                        left: e.clientX - offset.x,
                        top: e.clientY - offset.y - marginTop
                    });
                }
            }

            function handleMouseUp(e) {
                if (dragging) {
                    e.preventDefault();
                    console.log("stop dragging");
                    dragging = false;
                    $(document).off("mousemove", handleMouseMove);
                    $(document).off("mouseup", handleMouseUp);
                }
            }

            $header.on("mousedown", handleMouseDown);
        };

        const enabledServicesCache = new Map();
        const pendingCallsCache = new Map();

        /** Checks whether the named service is actually enabled for the current user, editor type and content URL. */
        ai.ifEnabled = function (service, callbackIfEnabled) {
            // console.log("AIConfig ifEnabled", service);
            const editorUrl = window.location.pathname;
            let contentURL = pages.editFrame.currentPath;
            const cachekey = editorUrl + "|||" + contentURL;
            const result = enabledServicesCache.get(cachekey);
            if (result) {
                if (result[service]) {
                    // console.log("AIConfig ifEnabled cached and true", service, cachekey);
                    callbackIfEnabled();
                } else {
                    // console.log("AIConfig ifEnabled cached and false", service, cachekey);
                }
            } else {
                const call = pendingCallsCache.get(cachekey) || $.ajax({
                    url: ai.const.url.general.config + ".json" + core.encodePath(contentURL),
                    type: "GET",
                    cache: false,
                    data: {editorUrl: editorUrl},
                    dataType: "json"
                }).fail((jqXHR, textStatus, errorThrown) => {
                    console.error("AIConfig ajaxError", jqXHR, textStatus, errorThrown);
                    debugger;
                }).done(data => {
                    console.log("AIConfig ifEnabled ajaxSuccess", service, editorUrl, contentURL, data);
                    if (data?.allowedServices) {
                        enabledServicesCache.set(cachekey, data.allowedServices);
                    } else {
                        console.error("AIConfig: Unexpected response", data);
                        debugger;
                    }
                }).always(() => {
                    pendingCallsCache.delete(cachekey);
                });
                pendingCallsCache.set(cachekey, call);

                call.done(data => {
                    if (data?.allowedServices) {
                        const allowed = data.allowedServices[service];
                        // console.log("AIConfig ifEnabled allowed", service, cachekey, allowed);
                        if (allowed) {
                            callbackIfEnabled();
                        }
                    }
                });
            }
        }

    })(window.composum.ai, window.composum.pages.dialogs, window.composum.pages, window.core, CPM.core.components);

})(window);

# Common conventions over all dialogs

## Submission in dialogs

The prompt field is a textarea where we want to be able to use the enter key to submit the form. It should be
possible to insert a new line for special prompts, but since we don't expect that to be a common case, the
convention should be that pressing Enter without any modifiers submits the form, and Shift+Enter creates a new line
instead.

## History module

The dialog history is specific to the component where a creation dialog is opened, and to the page where a side
panel dialog is used. In case of a creation dialog opened on a new Composum component dialog, we don't save the dialog 
history over the dialog life time as the new component has no identity yet.
For the dialog buttons we use the following classes that identify the buttons uniquely in the dialog:
composum-ai-reset-history-button , composum-ai-back-button , composum-ai-forward-button
We create a new entry on large changes (like overwriting the prompt with a predefined prompt or dialog reset) and
upon finishing the answer. Each time we check whether exactly that history has already been saved.

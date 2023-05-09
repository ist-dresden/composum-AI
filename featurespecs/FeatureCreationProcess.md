# Some ideas how to go on creating a feature with ChatGPT support.

In [ContentCreationDialog.md](ContentCreationDialog.md) there is an example feature that has been specified using a
dialog with ChatGPT, and later implemented with it's help. Here are some ideas how to do something like that.

The idea is to create a markdown file with the feature specification, and again and again give it to ChatGPT (likely
version >=4) to check for ideas, misunderstandings and so forth. Since it "thinks" quite differently than you do
yourself and is rather create, that's a pretty good feedback and support.

## Approach using the ChatGPT chat interface

In the [ChatGPT chat interface](https://chat.openai.com/) you can again and again edit the prompt, and thus have a
history without creating a huge discussion. (The ChatGPT dialogs actually have a tree structure where you can go
back to previous states of the discussion.) Or you can just edit the feature markdown file and paste the needed
segment into the ChatGPT dialog. To get alternatives remember to ask "Give me 10 variants for ..." or just
regenerate the response, which sometimes creates a completely different approach.

If you use markdown input, you can press the "copy to clipboard" button on the answer and easily copy out texts in
your feature documentation. A good idea is giving it examples of what you want, if you want a list of things.

## Steps creating a feature specification

1. Write a basic idea what the feature should do
2. Ask: "Name 20 additional things we could extend this feature with." The results can be used to extend the basic
   idea, fix some implementation decisions, and write an "Out of scope" section what not to do. All of these will
   support later steps.
3. You'll now likely have a spec that has sections "basic idea", "basic implementation decisions", "out of scope",
   "possible extensions". Submit all of that except possible extensions and ask: "To support the dialog design let's
   see some typical user workflows. Here are some likely usecases for the feature:"  That will likely lead to
   improvements of the "out of scope" and "basic implementation decisions".
4. When the usecases start to make sense, it's a good idea to collect the main intended usecases in a section "User
   Workflow". That can involve several turns of collecting and rewriting some usecases that seem fine and asking for
   more.
6. After completing a list of usecases you'll have a clear idea of the feature. Now ask for the needed dialog elements.

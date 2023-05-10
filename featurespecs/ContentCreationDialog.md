# Feature specification of the Content Creation Dialog

## Basic idea

A page in Composum Pages consists of various components where many of them contain text attributes. For instance a
teaser for another page (containing a title, subtitle and a text) or a text component that has a subtitle and a text, or
a section component that contains a title and other components. We want to create a content creation dialog that can be
opened from any of the attribute textfields or textareas or richtext editors in the component dialog and support
creating content for that attribute, which will replace the attribute content if the user presses "Replace" in the
dialog  
or be appended to the attribute content when the user presses "Append" on the dialog. It should provide a good
balance of being flexible to use, and not being too complicated.

The content creation itself is to be implemented using the ChatGPT completion API. The user can specify
ChatGPT prompts in various ways, and include existing text from the page as additional input for ChatGPT, so that
ChatGPT can be used to create content from a prompt, extend content, summarize content, excerpt content, make
suggestions to improve the text, title generation and more.

## Basic implementation decisions

- The content creation dialog should contain all necessary elements from the beginning - it should not be a flow of
  several dialogs, but a single dialog the user works in until they are satisfied.
- For textareas and richtext there is an optional drop down list to give an indication of the wanted text length with
  some rough indication of the desired text length. For textfields (a single line of content) that is absent.
  Options for this drop down list would be: "one line", "one sentence", "one paragraph", "several paragraphs".
- For now we will just request one variant at a time from ChatGPT.
- For selecting the content to include as additional input for ChatGPT, a drop down list with the following options: it
  could include the current text of the edited attribute, the text of the edited component (and subcomponents, if
  applicable) and the full text of the whole page. This is the only way to provide existing content.
- We will make a history for the field that has the ChatGPT suggestion, as the user will very likely want to switch
  back and forth between the texts it generated and take the best one. For that we will need back and forth buttons.
  The history is cleared on each new dialog start.
- The dialog has to be resizeable to work with large amounts of text.
- There should be a loading indicator that shows whether we are currently waiting for a ChatGPT response. Stati:
  idle (= used also when done), processing.
- We will not allow the user to edit the ChatGPT response as this can be edited in the text field.
- For better usability, tooltips or help texts could be added to explain each feature of the dialog.

## Out of scope

We will currently not include 'temperature' and 'max tokens' settings. The wanted text length can be specified by
the user in the prompt. We don't include a history and also no undo feature.

## User Workflow

To support the dialog design let's see some typical user workflows. Here are some likely usecases for the feature:

1. **Content Generation:** The user wants to create new content for a blank text field. They open the content creation
   dialog, write a prompt in the prompt field and select the desired text length from the dropdown menu. They then
   press "Replace" and the generated content is added to the text field.
2. **Title Generation:** The user wants to create a title for a section or page based on the content of that section
   or page. They can open the content creation dialog on the title textfield, select the option to include the text of
   the edited component or the full text of the page, select "summary" the prompt, and then press "Replace". The
   generated title replaces the existing title.
3. **Content Summary:** The user wants to create a summarized version of a longer text, for example, for a title,
   subtitle, or summary paragraph. They open the content creation dialog, select the content to summarize (e.g. the
   current page text or the text of the current component, especially if it's a section with subcomponents), select
   "summary" and the intended length from the drop down menus. After pressing "Replace", the generated summary replaces
   the long text in the text field.
4. **Content Extension:** The user has already written a portion of the content but needs help extending it. They open
   the content creation dialog, write a continuation prompt in the prompt field, and select the option to include
   the current text of the edited attribute. They then select the desired text length from the dropdown menu and
   press "Append". The generated content is appended to the existing content in the text field. Alternatively, they
   could choose "extend" from the list of predefined prompts. If the current text was to be replaced, since it was
   e.g. key points to be replaced by a full text, then the user would press "Replace".
5. **Content Improvement:** The user is not satisfied with the written content and wants suggestions to improve it.
   They open the content creation dialog, write a prompt asking for improvements or select "improve" from the list
   of predefined prompts, and select the option to include the current text of the edited attribute. They then press
   "Replace", and the generated improved content replaces the existing content in the text field.
6. **Excerpt Generation:** The user wants to create an excerpt from a longer piece of content. They open the content
   creation dialog, write a prompt asking for an excerpt, and select the option to include the current text of the
   edited attribute or the full text of the page. They then press "Replace", and the generated excerpt replaces the
   existing content in the text field.
7. **Idea Generation:** The user is stuck on creating new content and needs some inspiration. They open the content
   creation dialog, write a general prompt in the prompt field related to the topic they need ideas on, and then
   press "Replace". The generated ideas or suggestions will replace the current text in the text field. They can
   continue to iterate on this until they find an idea that they like.

## Dialog Elements

Given these workflows, the content creation dialog could have the following elements:

- **Prompt Field:** A text field where the user can write a custom prompt for ChatGPT. It should be a text area that
  can contain multiple lines. This field could also include a dropdown menu with pre-defined prompts like "summary",
  "improve", "extend", "title generation", etc. that are suitable for various use-cases.

- **Content Selector:** A dropdown menu for selecting the content to include as additional input for ChatGPT. The
  options could include the current text of the edited attribute, the text of the edited component (and subcomponents,
  if applicable) and the full text of the whole page.

- **Text Length Selector:** For textareas and richtext editors, a dropdown menu to select the desired text length.
  Options could include "one line", "one sentence", "one paragraph", "several paragraphs". This option is absent for
  textfields (a single line of content).

- **ChatGPT Response Field:** A text area to display the content generated by ChatGPT. This should be a large,
  resizable area since the generated content can be quite long. We do not allow editing, since the user can edit
  that in the dialog he called the ChatGPT dialog from. The user can see the generated content here before deciding to
  replace or append it to the existing content.

- **History Navigation:** A pair of "Back" and "Forward" buttons that allow the user to navigate through the history of
  generated texts for the current session. This way, they can easily compare different generated texts and choose the
  one they like best.

- **"Replace" Button:** This button will replace the existing content of the attribute with the content generated by
  the AI.

- **"Append" Button:** This button will append the content generated by the AI to the existing content of the attribute.

- **"Generate" Button:** This button will generate the content based on the provided prompt and additional input
  settings. The generated content will be shown in the Preview Area.

- **"Cancel" Button:** This button will close the dialog without making any changes to the existing content.

- **Loading Indicator:** An indicator showing the current status of the ChatGPT response. This could be a simple spinner
  that shows when the API is processing and disappears when the response is ready. The stati could be: idle (= used also
  when done), processing.

- **Close/Cancel Button:** A button to close the dialog without applying any changes. This is useful if the user decides
  not to use the generated content after all.

- **Alert:** a normally hidden area that can contain error messages or warnings.

## Possible extensions

- Request several variants simultaneously
- temperature setting
- selection of desired tone and writing style, like in
  [Superpower ChatGPT](https://chrome.google.com/webstore/detail/superpower-for-chatgpt/amhmeenmapldpjdedekalnfifgnpfnkc)
- save parts of prompts for reuse (e.g. tone, writing style, general comments about the site, definitions, slogans)
- Some kind of templates: predefined structure descriptions for specific functions of the text
- Advanced Text Editing: Incorporate features such as grammar and spelling checks, readability analysis, and style
  suggestions, check whether it fits the intended tone
- Some kind of history, to go back to previous suggested variants and / or prompts
- automated linking to other pages / external content
- content suggestions reviewing the whole page.
- ai powered image selection
- show the actual request sent to ChatGPT somewhere for transparency / debugging.
- The dialog should save the last used settings (e.g., the chosen additional input and desired text length) for the next
  time the dialog is opened.

## Glossary

- **Component:** A reusable building block in Composum Pages, which can contain text attributes.
- **Attribute:** A property of a component, which can contain text.
- **Dialog:** In this context, the user interface for editing component attributes, and the proposed interface for interacting with the ChatGPT API.
- **ChatGPT Completion API:** The API used to generate text from prompts.
- **Prompt:** A text input that guides the AI in generating a specific type of text.
- **Replace/Append:** The actions to take with the generated text. Replace will change the current attribute text with the generated text, and append will add the generated text to the end of the current attribute text.
- **Textfield/Textarea/Richtext Editor:** Different types of input fields for text in Composum Pages.
- **Text Length:** A user-specified guideline for how long the generated text should be.
- **Additional Input:** Existing text that is used to give context to the AI when generating text.
- **History:** A record of generated texts for a specific attribute during a session of the Content Creation Dialog.
- **Session:** A single use of the Content Creation Dialog, from opening to either replacing/appending text or closing the dialog.
- **Loading Indicator:** A visual signal to show when the AI is processing a prompt and when it is ready.
- **Alert:** An area to display error messages or warnings.

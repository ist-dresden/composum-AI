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
- For now we will just request one variant at a time from ChatGPT.
- For selecting the content to include as additional input for ChatGPT, a drop down list with the following options: it
  could include the current text of the edited attribute, the text of the edited component (and subcomponents, if
  applicable) and the full text of the whole page. This is the only way to provide existing content.

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
- show the actual request sent to ChatGPT.

# Feature specification of the general sidebar AI Dialog

## Basic idea

A page in Composum Pages consists of various components where many of them contain text attributes. For instance a
teaser for another page (containing a title, subtitle and a text) or a text component that has a subtitle and a text, or
a section component that contains a title and other components.
Composum Pages provides a WYSIWYG editor where you can create, edit and arrange such components.
There is a right sidebar that is normally shown along with the edited page, and contains various tabs
containing forms or texts that are created from the current state of the component or page. We will integrate a new
tab with the AI dialog.

There is a mechanism to extract the text content of a page or component in Markdown format that can be used to present
text of the current component or current page to AI. The idea is to give the user a "swiss knife" type tool
that can do a wide variety of tools that can be implemented by giving AI instructions and
the text of the currently shown component or the currently shown page or both, or user provided text or instructions.

Examples of what can be done with the dialog would include: simply querying AI, grammar / orthography / style
check, generating Content suggestions based on the current page, asking questions about the content.

We also should make it possible to do a chat and ask followup questions.

## Out of scope

Possible later features which are excluded for now:

- Save the output of the AI for future reference or for use in their pages.
- Some kind of automatical modification based on the AI suggestions.
- Button in the preview mode that triggers the dialog.
- (later, perhaps) general background information saved in the site

## Input sources

The input for AI can come from the following places. Often several are active, but all are optional. Some are
only active depending where the dialog is called from.

- a predefined prompt
- instructions typed in from the user
- text content: user provided, the page text content, the component text content, the attribute text content, a URL
  from which we gather the text content

## Implementation decisions

The dialog is quite similar to the content creation dialog in that there is:

- a normally hidden alert area that can contain error messages or warnings. The text will be shown in red, so a
  label is not necessary.
- a content selector that selects the additional input text that the prompt can refer to, such as the current page
  or the edited component
- a drop down list with predefined prompts the user can choose from
- a prompt input field that is overwritten on selection of a predefined prompt but can be edited by the user
- a history navigation (back and forward) so that the user can navigate through the history of the current session
- a button for opening an help dialog
- there is a response field where the AI response is shown as HTML formatted from markdown output.
- To allow a dialog there will appear another prompt input field below the response field, that displays the next
  response.
- Each prompt field has a submit button.

The dialog is relatively long but narrow, so the response and prompt fields should cover the whole width and no more 
than 2 drop down lists can be beside each other.

## Use cases

As an example, a typical use of the dialog for a grammar and style check would be as follows.

1. The user selects a page or a component within a page to edit.
2. In the right sidebar, the user navigates to the new AI dialog tab.
3. In the content selector, the user selects the source of the text content to be checked. This could be from the current page, the component, the attribute text content, or a user-provided text.
4. From the drop-down list, the user selects a predefined prompt for grammar and style check. This selection overwrites the prompt input field.
5. The user reviews the prompt in the prompt input field. If necessary, the user may edit the prompt to better suit their needs.
6. The user presses the submit button. The AI processes the request and presents the grammar and style check results in the response field.
7. If further clarification or a follow-up request is needed, another prompt input field appears below the response field. The user can then enter their next request, and press the corresponding submit button.

## Dialog elements

Summarizing, the dialog has the following elements:

The AI dialog interface comprises the following elements:

1. **Alert Area**: A hidden area that will display error messages or warnings when necessary. Text within this area will appear in red.

2. **Content Selector**: A dropdown menu where users can select the source of the text content that they want the AI to analyze. Options might include: the current page, the edited component, the attribute text content, a user-provided text, or a URL.

3. **Predefined Prompts Dropdown**: A dropdown list containing predefined prompts or queries that users can select for common tasks. This could include prompts for grammar checks, style checks, generating content suggestions, etc.

4. **Prompt Input Field**: A text input field where the prompt selected from the predefined prompts dropdown is 
   displayed. Users can edit this prompt to suit their specific needs. There is a submit button next to it.

5. **History Navigation**: Forward and backward navigation buttons that allow users to traverse through the history of their current AI interaction session.

6. **Help Button**: A button that opens a dialog providing help and instructions on how to use the AI dialog interface.

7. **Response Field**: A section where the AI's responses are displayed. This area should be large enough to accommodate complex responses and it would be formatted as HTML from markdown output.

8. **Additional Prompt Input Field**: An extra text input field that appears below the response field when the AI has delivered its response. This allows users to ask follow-up questions or provide further instructions. There is a submit button next to it.

## Dialog layout

The dialog could look as follows (ASCII art):

---------------------------------------------------------------------
| Alert Area (hidden by default)                                    |
---------------------------------------------------------------------
| Content Selector | Predefined Prompts Dropdown                    |
---------------------------------------------------------------------
| History Navigation: Back | Forward |  Help Button                 |
---------------------------------------------------------------------
| Prompt Input Field                                  | Submit Button|
---------------------------------------------------------------------
| Response Field                                                    |
|                                                                   |
|                                                                   |
|                                                                   |
---------------------------------------------------------------------
| Additional Prompt Input Field                     | Submit Button |
---------------------------------------------------------------------


## Feature list in consideration for the initial prompt library

0. **Query AI**: Use AI to execute a user specified instruction, independent of the edited page.
1. **Grammar and Style Check**: Use AI to evaluate the text for grammatical errors, style consistency, or overused
   phrases, and suggest improvements.
2. **Content Suggestions**: AI could suggest new content or improvements to the existing content in the component
   or page, based on the current text.
3. **Component Summary**: For more complex components, AI can provide a short summary of the content, aiding in
   understanding at a glance.
5. **Readability Score**: Use AI to calculate a readability score and provide suggestions for improvement, based on
   best practices for online content.
7. **Formatting Suggestions**: AI could give suggestions on how to better format the text for improved readability,
   such as adding headers, bullet points, etc.
8. **Content Tone Analysis**: Use AI to analyse the tone of the content and suggest changes to better match the
   desired tone.
11. **Keyword Density Check**: AI can analyze the keyword density in the content, aiding in SEO optimization.
12. **Sentiment Analysis**: Use AI to evaluate the sentiment expressed in the text, providing insight into
    potential reader perception.
13. **Content Preview**: A feature where AI generates a brief preview or blurb of the page content, which could be
    useful for social media sharing or as a meta description.
14. **Subject Analysis**: A feature where AI identifies the main subjects or themes in the text content and
    provides a summary.
15. **AI Q&A**: For complex subjects, a feature where you can ask AI questions about the text content for
    better understanding.
20. **Content Curation**: Use AI to curate relevant additional content from your website or external sources based
    on the text content of the current component.
21. **Inconsistency Detector**: Detect inconsistencies in the content, like conflicting statements.
22. **Thesaurus Suggestions**: Provide synonyms to diversify the language used in the content.
23. **Fact-Checking**: Suggest facts that may need verification.
17. **Page Navigation Suggestions:** Suggest improvements for better page navigation.
19. **Accessibility Review:** Review the text for accessibility issues, such as confusing language.
25. **Content Optimization Tips:** Provide tips to make the content more appealing and effective.
27. **Voice Consistency Review:** Ensure the content maintains a consistent voice and tone.
29. **Heading/Subheading Suggestions:** Suggest improvements for headings and subheadings.
30. **Writing Style Suggestions:** Suggest improvements in the writing style based on the audience.
34. **Localization Suggestions:** Suggest changes to make the content more locally relevant.
35. **Audience Engagement Analysis:** Analyze the content for its potential to engage the audience.
36. **Content Flow Suggestions:** Suggest improvements for better content flow and organization.
37. **Jargon Explanation Suggestions:** Suggest explanations for any jargon used.
33. **LSI Keyword Suggestions:** Suggest LSI (Latent Semantic Indexing) keywords related to the content.

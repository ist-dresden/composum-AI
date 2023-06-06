# Feature specification of the general ChatGPT Dialog

## Basic idea

A page in Composum Pages consists of various components where many of them contain text attributes. For instance a
teaser for another page (containing a title, subtitle and a text) or a text component that has a subtitle and a text, or
a section component that contains a title and other components.
Composum Pages provides a WYSIWYG editor where you can create, edit and arrange such components.
There is a right sidebar that can be shown along with the edited page which can
contain forms or texts that are created from the current state of the component or page. Alternatively, we could
display the dialog from a button in the page preview.

There is a mechanism to extract the text content of a page or component in Markdown format that can be used to present
text of the current component or current page to ChatGPT. The idea is to give the user a "swiss knife" type tool
that can do a wide variety of tools that can be implemented by giving ChatGPT instructions and
the text of the currently shown component or the currently shown page or both, or user provided text or instructions.

Examples of what can be done with the dialog would include: simply querying ChatGPT, grammar / orthography / style 
check, generating Content suggestions based on the current page, asking questions about the content.

## Input sources

The input for ChatGPT can come from the following places. Often several are active, but all are optional. Some are 
only active depending where the dialog is called from.
- a predefined prompt
- instructions typed in from the user
- text content: user provided, the page text content, the component text content, the attribute text content
- general background information saved in the site

Moreover:


## Feature list in consideration for the initial prompt library

0. **Query ChatGPT**: Use ChatGPT to execute a user specified instruction, independent of the edited page.
1. **Grammar and Style Check**: Use ChatGPT to evaluate the text for grammatical errors, style consistency, or overused
   phrases, and suggest improvements.
2. **Content Suggestions**: ChatGPT could suggest new content or improvements to the existing content in the component
   or page, based on the current text.
3. **Component Summary**: For more complex components, ChatGPT can provide a short summary of the content, aiding in
   understanding at a glance.
5. **Readability Score**: Use ChatGPT to calculate a readability score and provide suggestions for improvement, based on
   best practices for online content.
7. **Formatting Suggestions**: ChatGPT could give suggestions on how to better format the text for improved readability,
   such as adding headers, bullet points, etc.
8. **Content Tone Analysis**: Use ChatGPT to analyse the tone of the content and suggest changes to better match the
   desired tone.
11. **Keyword Density Check**: ChatGPT can analyze the keyword density in the content, aiding in SEO optimization.
12. **Sentiment Analysis**: Use ChatGPT to evaluate the sentiment expressed in the text, providing insight into
    potential reader perception.
13. **Content Preview**: A feature where ChatGPT generates a brief preview or blurb of the page content, which could be
    useful for social media sharing or as a meta description.
14. **Subject Analysis**: A feature where ChatGPT identifies the main subjects or themes in the text content and
    provides a summary.
15. **ChatGPT Q&A**: For complex subjects, a feature where you can ask ChatGPT questions about the text content for
    better understanding.
20. **Content Curation**: Use ChatGPT to curate relevant additional content from your website or external sources based
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

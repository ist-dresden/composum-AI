# Feature ideas

This is just a wild unfiltered brainstorming type list of ideas that could be supported here, not (yet?) actually
planned. You might also find some features here that were already integrated.

## Sidebar features

A page in Composum Pages consists of various components where many of them contain text attributes. For instance a
teaser for another page (containing a title, subtitle and a text) or a text component that has a subtitle and a text, or
a section component that contains a title and other components.
Composum Pages provides a WYSIWYG editor where you can create, edit and arrange such components.
There is a right sidebar that can be shown along with the edited page which can
contain forms or texts that are created from the current state of the component or page.

There is a mechanism to extract the text content of
a page or component in Markdown format that can be used to present text of the current component or current page to
ChatGPT.

We now collect feature ideas that could be helpful for the page editor that could be shown in the sidebar and could be
implemented using the ChatGPT chat interface. That is: which can be implemented by giving ChatGPT instructions and
the text of the currently shown component or the currently shown page or both.

### Feature ideas that could be implemented

#### Features wrt. to the current page / component / other site specific information

1. **Grammar and Style Check**: Use ChatGPT to evaluate the text for grammatical errors, style consistency, or overused
   phrases, and suggest improvements.
2. **Content Suggestions**: ChatGPT could suggest new content or improvements to the existing content in the component or page, based 
   on the current text.
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
29. **Social Media Post Idea Generator**: Given a topic, ChatGPT could suggest creative social media post ideas.
31. **Slogan Generator**: ChatGPT could generate catchy slogans based on a given product or brand name.

#### Features without access to the current page.

This contains features that do not need access to the current page or component text. That is: features querying 
ChatGPT with predefined prompt plus an user specified prompt combined.

1. **Synonym Suggester**: ChatGPT could suggest synonyms for a specified word or phrase to enhance the variety of language used on the page.
2. **Content Idea Generator**: Given a specific topic, ChatGPT could generate content ideas for new components or pages.
3. **Phrase Simplifier**: ChatGPT could simplify a given sentence or phrase, making it easier to understand for a wider audience.
8. **Call-to-Action Phrase Generator**: ChatGPT could suggest effective call-to-action phrases.
9. **Bullet-to-Paragraph Converter**: Given a list of bullet points, ChatGPT could generate a text fragment. (This can 
   be done with the content creation dialog that is called on the editor, but might make sense before creating the 
   components.)
12. **Metaphor Generator**: ChatGPT could generate metaphors related to a given topic or idea.
13. **Analogies Creator**: Given a concept, ChatGPT could generate an analogy to help explain it better.
14. **Industry Jargon Explainer**: ChatGPT could provide simple explanations or definitions for industry-specific terms.
15. **Content Structure Suggester**: Given a content topic, ChatGPT could suggest a possible structure or layout.
16. **Quotation Generator**: Given a topic, ChatGPT could suggest relevant and engaging quotes.
22. **Inspirational Message Generator**: ChatGPT could generate motivational or inspirational messages related to a given theme.
23. **Humor Insertion**: Given a piece of text, ChatGPT could suggest ways to make it funnier.
37. **Value Proposition Generator**: Given a product or service description, ChatGPT could suggest a compelling value proposition.

### Feture ideas that are already present somewhere else in some form, and thus are probably not necessary

6. **Text Translation**: Leverage ChatGPT's language understanding to provide translations of the content, aiding in the
   creation of multilingual pages.
10. **Title/Headline Suggestions**: Provide alternative suggestions for titles and headlines to help capture attention
    and interest.

### Feature ideas that could be implemented but would belong somewhere else

19. **Text Simplification**: Use ChatGPT to simplify complex language or jargon, making the content more accessible to a
    wider audience.
16. **Content Categorization:** Categorize the content into various topics.

### Feature ideas that are difficult to implement

17. **Suggested Links**: Use ChatGPT to suggest internal or external links that could be added to the content for
    further context or information.
4. **SEO Evaluation**: ChatGPT can review the text in terms of SEO, suggesting better keywords or ways to improve search
   engine performance.
9. **A/B Content Testing**: Utilize ChatGPT to suggest variations of the text content for A/B testing. 
10. **Content Freshness Indicator**: Determine if the content appears outdated and needs updating.
19. **Competitor Content Analysis**: A feature where ChatGPT can compare your page content to similar content from
    competitors and suggest improvements. 
25. **Blog Post Idea Generator**: Given a general topic, ChatGPT could generate specific blog post ideas.
27. **Rephrase Generator**: Given a sentence, ChatGPT could rephrase it to enhance clarity or creativity.

### Remote ideas for the future

- ChatGPT role playing as some kind of user, clicking on links etc. to find some information
- **Content Comparisons**: Users can compare different versions of a component or page with ChatGPT and get 
  recommendations on which version is more effective or engaging.
- Summarize content release changes
- Categorize user feedback
- Training tool

## ChatGPT input sources

The input for ChatGPT can come from the following places. Often several are active, but all are optional. Some are 
only active depending where the dialog is called from.
- a predefined prompt
- instructions typed in from the user
- text content: user provided, the page text content, the component text content, the attribute text content, text 
  selected when calling up the dialog
- general background information saved in the site
- user specified general preferences (style, tone, target audience)
- site metadata: structure, existing pages
- other pages from the site
- retrieve from dialog history

## ChatGPT output destinations

The output can go into the following places:
- just displayed to the user
- clipboard
- replace / append to currently edited text
- save for later in a dedicated area
- automated suggestions during typing
- user notifications
- automated content update suggestions

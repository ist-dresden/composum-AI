# Some ideas how to go on creating a feature with ChatGPT support.

In [ContentCreationDialog.md](1ContentCreationDialog.md) there is an example feature that has been specified using a
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
   see some typical user workflows. Here are some likely use cases for the feature:"  That will likely lead to
   improvements of the "out of scope" and "basic implementation decisions".
4. When the use cases start to make sense, it's a good idea to collect the main intended use cases in a section "User
   Workflow". That can involve several turns of collecting and rewriting some use cases that seem fine and asking for
   more.
5. After completing a list of use cases you'll have a clear idea of the feature. Now ask for the needed dialog
   elements. Probably it'll take some turns fixing parts of the spec until the list seems right. Recheck with "Please
   check whether there are missing dialog elements, and whether these dialog elements fit the usage in the use cases."
6. Have ChatGPT render suggestions for the dialog layout and incorporate these into the design document.

(To be continued.)

An example for this is the [specification for the content creation dialog](1ContentCreationDialog.md) which has been
created with this process.

### Some specific prompts

#### Dialog layout

These prompts can help to get a visual overview (wireframe)
The SVG variant is quite useable to quickly display a design idea but seems to need several tries to get it
right. A HTML figure is also nice input, easy to get right and close to the actual dialog, however to display it
even on Github it needs to draw from a restricted set of attributes - Github removes many tags.

##### render as Github markdown

A wireframe of a dialog can be rendered as a raw HTML fragment in Github Markdown, but we need to specify which
elements are permitted by the
[Github sanitization whitelist](https://github.com/gjtorikian/html-pipeline/blob/main/lib/html_pipeline/sanitization_filter.rb).
Rendered like that the result can be included into the final documentation, but some things look not quite normal
since e.g. button and select is forbidden for an unknown reason.

> Please create a HTML table as a raw HTML fragment in Github markdown, that shows this dialog design as a wireframe.
> Only the following HTML elements are permitted: h1, h2, h3, h4, h5, h6, br, b, i, strong, em, a, img, div, p, ol, ul,
> li, table, thead, tbody, tfoot, tr, td, th, caption, blockquote, dl, dt, dd, hr, summary, details, figure,
> figcaption, abbr, cite, mark, small, span, time.
> The elements button, input, textarea, select and option are explicitly forbidden - their appearence has to be
> approximated using other elements.
> Only the following attributes of HTML elements are permitted: accept, align, alt, aria-describedby, aria-hidden,
> aria-label, aria-labelledby, border, charset, checked, cols, colspan, coords, datetime, dir, disabled, for, headers,
> height, hreflang, id, label, lang, maxlength, media, method, name, open, readonly, rel, role, rows, rowspan, selected,
> shape, size, span, src, start, summary, tabindex, title, type, value, width.
> CSS and the style attribute is forbidden, and all
> Visibly render the dialog frame and internal frames and dividers, e.g. with nested tables with a border.
> Use the align and valign attributes for alignments.
> Render icons like e.g. [X] for the close icon, without showing their names.
> The names of groups and subgroups should not be shown, only labels that should appear in the fully
> implemented dialog.
> For the elements like buttons, input fields, text areas etc. you should use the corresponding HTML elements as far
> as permitted.
> The dialog should look as closely as possible to the fully implemented dialog, while observing these conditions.
> After discussion of the layout output a single code block with the HTML with the table element, no
> surrounding HTML or BODY tag. No discussion after the HTML block.

##### render as HTML

For rendering a HTML that can be inspected in a browser or, e.g., in the IntellJ markdown editor (but doesn't render
well on Github), you can use:

> Please create a HTML table that shows this dialog design as a wireframe.
> To render the dialog frame and internal frames and dividers, use the HTML table, tr and td attributes,
> but no CSS unless absolutely necessary.
> Use the align and valign attributes for alignments.
> For drop down lists include the options, as far as they are already specified.
> Render icons like e.g. [X] for the close icon, without showing their names.
> You can use nested tables with a border to create nested frames for the subgroups.
> The names of groups and subgroups should not be shown, only labels that should appear in the fully
> implemented dialog.
> For the elements like buttons, input fields, text areas etc. you should use the corresponding HTML elements.
> The dialog should look as closely as possible to the fully implemented dialog, while observing these conditions.
> Do not output any comments or explanations, just a single code block with the HTML with the table element, no
> surrounding HTML or BODY tag.

##### render as ASCII art

There is always ascii art to get a very quick overview:

> Please create an ascii art of the dialog, rendered as markdown code block with 4 spaces indentation.
> Buttons should be rendered like [Cancel] when "Cancel" is the text on them, so that the layout is nicely shown.
> Drop down lists can be rendered like [\/Predefined].
> Text fields, Text areas should be shown with a description what is in there, spaces rendered as _, and with more _
> showing the full space they occupy. (For text areas that will be several lines.)
> Otherwise the dialog should look as closely as ascii art can make it to the fully implemented dialog.
> The names of groups and subgroups should not be shown, except if they should appear in the fully implemented dialog.
> No explanation is necessary, please render just a drawing of the dialog in a ascii art code block.

##### render as SVG

SVG is more or less useable and looks better, but has sometimes bugs.

> Please create a code block with a SVG representation of the dialog, that could be rendered by a browser to display
> a suggestion for the dialog.
> The dialog should have a frame, the subgroups also should have a frame.
> The names of groups and subgroups should not be shown, except if they should appear in the fully implemented dialog.
> The text fields and text areas should be rendered as a frame, with a descriptive text shown inside.
> Render buttons and drop down lists with a frame, and indicate with a suitable symbol the drop down list.
> No explanation is necessary, please render just a drawing of the dialog in a SVG code block.
> Please output only the svg tag and the svg elements, insert no comments into the SVG text, and take care to create a
> valid SVG including the xmlns declaration.

## Other things you could have ChatGPT check

- "Please check the page for contradictions."
- "Please check this text for consistent use of terminology."
- "Please check this text for grammar or orthografical errors and inexact language."
- "Please create a glossary of important terms."
- "Please check for types of redundant information and in which sections these redundancies occur."

### In development:

#### introductory prompt:

ChatGPT: Please help me create a specification for a feature that extends our existing application Composum Pages.
I'll give you the specification as it is right now in the next message. If I have a request for you that is not part of
the specification, I'll prefix it with `ChatGPT:`, as in this message.
After I give you the start of the specification, please make a suggestion how to continue the text of the specification.
If you have suggestions that should not be part of the specification, please prefix them with "Comment:".

ChatGPT: Important: do only briefly describe visual structure of the dialog, take care not repeat the function of the
elements that was already described. Specify the texts for labels, buttons or text areas and mention
whether a group or subgroup should have a label. Be brief, e.g. `**Cancel Button:** with label "Cancel", positioned
left of the "Accept" button.`

#### make excerpt for developer

Please make an excerpt of this specification with what is important for implementing this. Please omit discussions, the
reasons why it was specified that way, repetitions and redundancies - just what a developer would need to know about
this feature to implement it. Do not include general discussions an experienced developer would know, no general
guidelines, just information about this feature.

# Planning for the Demo video(s)

Screen recordings with QuickTime , cut with iMovie
Compression with Handbrake 'Fast 1080p30' + web optimized.
Create picture of first frame with
ffmpeg -i QuickDemo.mp4 -vframes 1 QuickDemo-poster.png
!! Filename of picture shouldn't be exactly as movie

## Composum

### Quick Composum Demo

Intro Picture?
9 - Translation2.mov - Translate
7 - Tag1.mov - Tag
3 - GenerateIdeas4.mov - Collect Ideas ; Chat with the AIU
2 - Conclusion.mov - Generate
8 - Transform.mov - Transform
5 - PromptLibrary.mov - Prompt Library
6 - Review.mov - Review your content
1 - Background Knowledge.mov - Using background knowledge of the AI

4 - Fixed

### Chat demo ideas

https://cloud.composum.com/bin/pages.html/content/ist/composum/home/pages/setup
Give me a docker command line that starts pages on port 9090 in the background.

https://cloud.composum.com/bin/pages.html/content/ist/composum/home/blog/pages/composumAI
Make 10 suggestions for short (max. 10 second) video clips that demonstrate the capabilities of the AI described in this
Blog.

Make 10 suggestions for headlines of max. 3 words that describe what happens in the clips. For instance: "Create
Ideas", "Translate", "Tag"

Make 10 suggestions for headlines of max. 3 words that describe what happens in the clips. For instance: "Create
Ideas", "Translate", "Tag"

Create descriptions of suggestions for short (max. 10 second) video clips that demonstrate these capabilities of the AI.

### AEM Demo

http://localhost:4502/sites.html/content/wknd/language-masters/en

Wichtige features: page description, richtext editor in text, textarea and richtext in dialog, richtext in
experience fragment, chat, history
Relevant components: teaser, text, embed (HTML)
?? make a table / JSON
?? wichtige prompts:

- rich text editor in text http://localhost:4502/sites.html/content/wknd/language-masters/en - intro
- page properties http://localhost:4502/sites.html/content/wknd/language-masters/en - abstract
- content transformation:
    - Create a table with the data from the text
    - http://localhost:4502/editor.html/content/wknd/language-masters/en/about-us.html Make a list of contributor names
- teaser description
- copy in web page, summarize
- chat:
    - FAQ:
        - Give 5 suggestions in the form of a FAQ that could be answered - only the question.
        - Suggest an answer for question 4

Snippets for AEM Quick Demo:
[AEMSidebarAndContentCreation.png](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEMSidebarAndContentCreation.png)
Intro picture, title "Composum AI for AEM"
[AEM-Text-Generate-Content.mov](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEM-Text-Generate-Content.mov)
"Create Content" - content creation dialog on inline text, incl. page length
[AEM-Dialog-Teaser-External-Content.mov](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEM-Dialog-Teaser-External-Content.mov)
"Transform external content" - content creation dialog from dialog, external input
[AEM-Page-Description-incl-History.mov](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEM-Page-Description-incl-History.mov)
"Summarize" , "Choose from History"
[AEM-Content-Transformation-Table.mov](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEM-Content-Transformation-Table.mov)
"Rewrite" - content creation dialog on inline text
[AEM-Configurability.mov](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEM-Configurability.mov)
title: "Configurable"
[AEM-Chat.mov](..%2Farchive%2Fvideos%2Faemquickdemo%2Fparts%2FAEM-Chat.mov)
Side panel AI, title: "Side Panel AI: analyze, discuss, suggest"

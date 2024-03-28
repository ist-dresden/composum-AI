# Important ideas to convey in talk

## LLM interface: sehr einfach aber extrem flexibel

Request in general:

- System message: what it fundamentally should do
- Data:
    - possible background information
    - text to be processed
- Prompt: instructions on what to do exactly

Example for Translation:

- System message: "You are translation expert"
- Data: "Georg Fischer is proud of its long history of success."
- Prompt: "Translate the sentence into English." -> "Georg Fischer is proud of its long history of success."

Example for Text Generation:

- System message: "You are a professional editorial assistant."
- Data: "With Specialized Solutions ...."
- Prompt: "Create 10 very different suggestions for a headline."
- -> "Succeed with Specialized Solutions: 10 reasons why you can trust us.", "Optimize your projects with Specialized
  Solutions" ...

Many possibilities for specific tasks:

- Translation
- Text generation - Suggestions for: summary, suggestions for text fragments: rephrasing, CTA, slogans, teasers, image
  descriptions, SEO, transformation, ...
- Text analysis: gap analysis, evaluation, suggestions for restructuring, ...
- Search: selection of the most suitable search results, possibly direct answers to natural language queries

"AIs are just humans too" - Execution of manually described subtasks.
(Mental model of a human assistant with huge general knowledge but no knowledge about the given task is better
model than computer program. "Natürlichsprachliche Programmierung")

## Composum AI

Various assistants to support these processes:

- Content Creation Assistant kurze Vorführung Grundidee, evtl. als Beispiel CTA suggestions, headline suggestions,
  generate Teaser
- Sidebar AI

- Demo of automatic translation POC

## Advantages of using LLM for translation

- all texts of a page are translated together - can improve consistency of resulting text
- use Background information like intended style, tone and formality
- use list of intended translations of special terms
- use previous manual corrections to improve translation of changes

## Possible project scopes

- full implementation automatic translation
- exploration + adaption of the existing possibilities of Composum AI for content creation
- support in finding ideas for LLM support for editor workflows

Kennst Du die Rephrasierungsvorschläge / Wortvorschläge in https://www.deepl.com/write ? Das wäre auch ein 
super-cooles Tool das man für Redakteure replizieren könnte. (Allerdings muss man da in den Richtexteditor eingreifen.
:-( )

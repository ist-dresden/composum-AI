# Wichtige Ideen

## LLM interface: sehr einfach aber extrem flexibel

Request:
  - Systemmessage: was es grundsätzlich machen soll
  - Daten:
    - evtl. Hintergrundinformationen
    - zu bearbeitender Text
  - Prompt: Instruktionen was zu tun ist

Beispiel Übersetzung:
  - Systemmessage: "Sein ein Übersetzungsexperte"
  - Daten: "Georg Fischer ist stolz auf seine lange Erfolgsgeschichte."
  - Prompt: "Übersetze den Satz ins Englische."
-> "Georg Fischer is proud of its long history of success."

Beispiel Textgenerierung:
  - Systemmessage: "Sei ein professioneller Redaktionsassistent."
  - Daten: "Mit Spezialized Solutions ...."
  - Prompt: "Erstelle 10 sehr unterschiedliche Vorschläge für eine Überschrift."
-> "Mit Spezialized Solutions zum Erfolg: 10 Gründe, warum Sie uns vertrauen können."
   "Optimieren Sie Ihre Projekte mit Spezialized Solutions" ...

Viele Möglichkeiten für konkrete Aufgaben:
- Übersetzung
- Textgenerierung - Vorschläge für: Zusammenfassung, Vorschläge für Textfragmente: Umformulierung, CTA, Slogans, 
  Teaser, Bildbeschreibungen, SEO, Transformation, ...
- Textanalyse: gap analysis, suggestions for restructuring, ...
- Suche: Auswahl der passendsten Suchresultate, evtl. direkte Beantwortung von natürlichsprachlichen Anfragen

"Eine KI ist auch nur ein Mensch" - Ausführung von konkret beschriebenen Teilaufgaben

## Composum AI

Various assistants to support these processes:
- Content Creation Assistant kurze Vorführung Grundidee, evtl. CTA suggestions, headline suggestions, 
  - ? content transformation
- Sidebar AI

- Übersetzung Vorführung

## Advantages of using LLM for translation

- all texts of a page are translated together - can improve consistency of resulting text
- use Background information like intended style, tone and formality
- use list of intended translations of special terms
- use previous manual corrections to improve translation of changes

## Possible project scopes

- full implementation automatic translation
- exploration + adaption of the existing possibilities of Composum AI for content creation
- support in finding ideas for LLM support for editor workflows

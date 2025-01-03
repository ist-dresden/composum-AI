#!/usr/bin/env aigenpipeline
-m gpt-4o
../../../../../../../../../../../featurespecs/8.2AutomaticTranslationMergeTool.md
-p list.html.prompt
-upd -o list.html

-m gpt-4o
../../../../../../../../../../../featurespecs/8.2AutomaticTranslationMergeTool.md
list.html
-p list.css.prompt
-upd -o list.css

-m gpt-4o
../../../../../../../../../../../featurespecs/8.2AutomaticTranslationMergeTool.md
list.html
-p list.js.prompt
-upd -o list.js


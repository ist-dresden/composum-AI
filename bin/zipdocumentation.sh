#!/usr/bin/env bash
echo create a zip of the documentation for use in a OpenAI GPT as knowledge base
mdfiles=$(find . -name '*.md' | egrep -v -i 'node_modules|hpsx|/\.|src/test|archive')
# echo "$mdfiles"
pngfiles=$(find . -name '*.png' | egrep -v -i 'node_modules|hpsx|/\.|vault|cpn-')
# echo "$pngfiles"
# create a zip composumaidocs.zip from the all these files
zip -r composumaidocs.zip $mdfiles $pngfiles archive/logos/ComposumAIFeatherWide3s.jpg
echo "created composumaidocs.zip"

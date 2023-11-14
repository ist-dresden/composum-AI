#!/usr/bin/env bash
echo Perform a link check for the site https://ist-dresden.github.io/composum-AI/

mvn -N clean site

#brew upgrade linklint # MacOS!
#mkdir -p target/linkcheck/.
#mkdir -p .linklint
#linklint -cache .linklint -root target/site/. -host ist-dresden.github.io -net -doc target/linkcheck/. -out target/linkcheck/linklint.log /#/#

# needs to be installed with pip install linkchecker
# linkchecker linklint -http -host https://ist-dresden.github.io/composum-AI/ /#/# -doc target/linkcheck/.

# for MacOS
brew upgrade lychee
lychee --cache -f detailed --base /composum-AI/ \
  --exclude '.*composum-ai.*/index.html' --exclude '../index.html' \
  --exclude 'https://cloud.composum.com/' \
  --exclude 'https://chat.openai.com/' \
  --exclude 'http://localhost:8080/system/console/configMgr' \
  target/site/*/*.html

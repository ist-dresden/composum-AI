#!/usr/bin/env bash
echo Perform a link check for the site:stage of https://ist-dresden.github.io/composum-AI/

# for MacOS
brew upgrade lychee
CMDLINE="lychee --cache -f detailed --base target/staging/parent-2/parent-2-public/composum-platform/composum-ai-integration/ \
  --exclude '.*composum-ai.*/index.html' --exclude '../index.html' \
  --exclude 'https://cloud.composum.com/' \
  --exclude 'https://chat.openai.com/' \
  --exclude 'http://localhost:8080/system/console/configMgr' \
  target/staging/parent-2/parent-2-public/composum-platform/composum-ai-integration/*.html \
  target/staging/parent-2/parent-2-public/composum-platform/composum-ai-integration/*/*.html"


mvn clean install javadoc:aggregate site site:stage

$CMDLINE

$CMDLINE >& target/linkcheckfull.log

echo LOGFILE: target/linkcheckfull.log

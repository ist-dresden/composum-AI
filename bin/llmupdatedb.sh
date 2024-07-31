#!/usr/bin/env bash
echo update database for llmsearch.sh
echo large language model based search
progfile=$0
if test -L "$progfile"; then
  progfile=$(readlink "$progfile")
fi
progdir=$(dirname "$progfile")/..
cd $progdir

mkdir -p .cgptcodeveloper

# STORE="--store"
STORE=""

set -vx
llm embed-multi md -d .cgptcodeveloper/llmsearch.db -m minilm $STORE --files . '**/*.md'
llm embed-multi java -d .cgptcodeveloper/llmsearch.db -m minilm $STORE --files . '**/src/**/*.java'
llm embed-multi js -d .cgptcodeveloper/llmsearch.db -m minilm $STORE --files . '**/src/**/*.js'
llm embed-multi html -d .cgptcodeveloper/llmsearch.db -m minilm $STORE --files . '**/src/**/*.html'
llm embed-multi jsp -d .cgptcodeveloper/llmsearch.db -m minilm $STORE --files . '**/src/**/*.jsp'

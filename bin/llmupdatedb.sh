#!/usr/bin/env bash
echo update database for llmsearch.sh
echo large language model based search
progfile=$0
if test -L "$progfile"; then
  progfile=$(readlink "$progfile")
fi
progdir=$(dirname "$progfile")/..
cd $progdir

# STORE="--store"
STORE=""

set -vx
llm embed-multi md -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/*.md'
llm embed-multi java -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.java'
llm embed-multi js -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.js'
llm embed-multi html -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.html'
llm embed-multi jsp -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.jsp'

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

llm embed-multi til -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/*.md'
llm embed-multi til -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.java'
llm embed-multi til -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.js'
llm embed-multi til -d .cgptdevbench/llmsearch.db -m minilm $STORE --files . '**/src/**/*.html'

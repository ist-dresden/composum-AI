#!/usr/bin/env bash
# Script that uses aigentool to greate embedsearch.html

MYDIR=`dirname $0`

# goto project home
cd $MYDIR/../../../../../../../../../../../../..
# exit if there is no .git - there is something wrong
[[ -z "$(git rev-parse --git-dir 2>/dev/null)" ]] && exit 1

# DIR should be the relative path of MYDIR to current dir
DIR=$(realpath --relative-to="." "$MYDIR")

set -x
# model at least -m "gpt-4-turbo" or equivalent
aigenpipeline -p $DIR/embedsearch.prompt -o $DIR/embedsearch.html $DIR/embedsearch.html backend/slingbase/src/main/java/com/composum/ai/backend/slingbase/impl/RAGServlet.java

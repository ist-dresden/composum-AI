#!/usr/bin/env bash
# Script that uses aigentool to greate rag.html

cd `dirname $0`

# goto project home
PHOME=../../../../../../../../../../../../..
# exit if there is no directory named .git there - there is something wrong
# if file $PHOME/.git does not exist, exit
if [ ! -d "$PHOME/.git" ]; then
    echo "No .git directory found in $PHOME"
    exit 1
fi

set -x
# model should be at least -m "gpt-4-turbo" or something comparable
aigenpipeline -p rag.prompt -o rag.html rag.html $PHOME/backend/slingbase/src/main/java/com/composum/ai/backend/slingbase/impl/RAGServlet.java

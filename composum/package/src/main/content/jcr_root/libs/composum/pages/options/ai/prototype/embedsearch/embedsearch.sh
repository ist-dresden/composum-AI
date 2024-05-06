#!/usr/bin/env bash
# Script that uses aigentool to greate embedsearch.html

cd `dirname $0`

# project home
PHOME=../../../../../../../../../../../../..
# exit if there is no directory named .git there - there is something wrong
# if file $PHOME/.git does not exist, exit
if [ ! -d "$PHOME/.git" ]; then
    echo "No .git directory found in $PHOME"
    exit 1
fi

set -x
# model at least -m "gpt-4-turbo" or equivalent
aigenpipeline -p embedsearch.prompt -o embedsearch.html embedsearch.html $PHOME/backend/slingbase/src/main/java/com/composum/ai/backend/slingbase/impl/RAGServlet.java

cp -f embedsearch.html $PHOME/aem/ui.apps/src/main/content/jcr_root/apps/composum-ai/prototype/embedsearch/

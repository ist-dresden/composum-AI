#!/usr/bin/env bash
echo "Generating code... (using https://aigenpipeline.stoerr.net/)"
cd $(dirname $0)/..

composum/package/src/main/content/jcr_root/libs/composum/pages/options/ai/prototype/embedsearch/embedsearch.sh
composum/package/src/main/content/jcr_root/libs/composum/pages/options/ai/prototype/rag/rag.sh

echo Dependencies:
aigenpipeline -dd -os '**/src/**/*'
echo
echo Generating code...
aigenpipeline -os '**/src/**/*'

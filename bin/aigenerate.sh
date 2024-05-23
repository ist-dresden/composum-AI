#!/usr/bin/env bash
echo "Generating code... (using https://aigenpipeline.stoerr.net/)"
cd $(dirname $0)/..
echo Dependencies:
aigenpipeline -dd -os '**/src/**/*'
echo
echo Generating code...
aigenpipeline -os '**/src/**/*'

#!/usr/bin/env bash
cd $(dirname $0)/..
aigenpipeline -dd -os '**/src/**/*'
aigenpipeline -os '**/src/**/*'

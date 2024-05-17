#!/usr/bin/env bash
cd $(dirname $0)/..
aigenpipeline -dd -os 'backend/slingbase/**/main/**/*'
aigenpipeline -os 'backend/slingbase/**/main/**/*'

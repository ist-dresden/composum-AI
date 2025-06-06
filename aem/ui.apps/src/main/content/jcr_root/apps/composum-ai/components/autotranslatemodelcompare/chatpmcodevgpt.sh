#!/usr/bin/env bash
echo start chat for working on this.

gitdir=$(git rev-parse --show-toplevel)
thisdir=$(dirname $(realpath "$0"))
reldir=$(grealpath --relative-to="$gitdir" "$thisdir")

cd "$gitdir" || exit 1
exec pmcodevgpt -ff $reldir/*.html $reldir/*.css $reldir/*.js -f $reldir/README.md aem/core/src/main/java/com/composum/ai/aem/core/impl/autotranslate/AutotranslateModelCompareModel.java "$@"

#!/usr/bin/env bash
echo start chat for working on this.

gitdir=$(git rev-parse --show-toplevel)
thisdir=$(dirname $(realpath "$0"))
reldir=$(grealpath --relative-to="$gitdir" "$thisdir")

cd "$gitdir" || exit 1
exec chatgpt -m o3-mini -o reasoning_effort=high -ff $reldir/*.html $reldir/*.css $reldir/*.js $reldir/specification.md aem/core/src/main/java/com/composum/ai/aem/core/impl/autotranslate/AutotranslateModelCompareModel.java -ocf $CHATGPTTOOLS/exampleactions/chatgptpmcodev.cfg "$@" "check Specification.md and compare with the retrieved files from this directory and adapt the code to the specification. If you find any errors in the code, please fix them. If you find any errors in the specification, please report them. Take care to only make changes if necessary for this task. Write the files or changes into this directory instead printing them."

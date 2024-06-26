#!/usr/bin/env bash
# echo search using large language model for "$*"
# update database using bin/llmupdatedb.sh

progfile=$0
if test -L "$progfile"; then
  progfile=$(readlink "$progfile")
fi
progdir=$(dirname "$progfile")/..
cd $progdir

key=$1
shift
echo searching at key $1 for "$*"

# if .cgptdevbench/llmsearch.db does not exist or is older than a week
# then update it using bin/llmupdatedb.sh
if [[ ! -f .cgptdevbench/llmsearch.db ]] || [[ $(find .cgptdevbench/llmsearch.db -mtime +7) ]]; then
  # echo starting database update
  bin/llmupdatedb.sh >& /dev/null
  # echo finished database update
fi

llm similar $key -d .cgptdevbench/llmsearch.db -n 20 -c "$*" | fgrep -v /node_modules/

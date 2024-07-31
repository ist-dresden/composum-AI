# Plugin Action: execute a LLM based search for the search string given as input
# echo executing a LLM based search
searchstring=$(cat)
echo "Searching for \"$searchstring\""

for type in md java js html jsp; do
  bin/llmsearch.sh "$type" "$searchstring"
done | fgrep '{' | jq -c -s 'map(del(.[] | select(. == null))) | sort_by(.score) | reverse[]' | head -n 20

echo ...

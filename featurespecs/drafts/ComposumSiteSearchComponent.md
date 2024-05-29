# Integration example RAG search

(TODO; not started)

Composum Site search

- Searchfield: component composum/pages/components/search/field (No changes needed.)
- Search results page: e.g. /content/ist/composum/meta/search 
  - search field: composum/pages/components/search/field
  - search results: composum/pages/components/search/result


# Obtain CSRF Token
csrf_token=$(curl -u admin:admin -X GET http://localhost:4502/libs/granite/csrf/token.json | jq -r .token) ; echo $csrf_token

# Make POST Request with CSRF Token
curl -u admin:admin -X POST -H "CSRF-Token: ${csrf_token}" -H "Content-Type: application/json" -d '{"your":"data"}' http://localhost:4502/bin/cpm/ai/experimental/templating.resetToPrompts.json

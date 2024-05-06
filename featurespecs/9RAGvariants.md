# Retrieval Augmented Generation (RAG) Variants

Status: prototypical mini implementation - a servlet and experimental UI <br/>
Composum: http://localhost:9090/libs/composum/pages/options/ai/prototype.html <br/> 
AEM: http://localhost:4502/apps/composum-ai/prototype.html

## Background

We assume that we have an informational website with static content.

## Basic idea

We want to combine a text search with LLM RAG, possibly with vector (embedding) search, possibly not. Ideas for that:

- Intelligent search that uses a LLM to improve search results
- Answer questions in a chat better by supporting the LLM with a RAG search

## Basic implementation decisions

- Instead of a vector database we calculate embeddings on demand, possibly with a cache.

## Out of scope

We do not want to employ a separate vector database for RAG, but see what we can do without that.

## Implementation

### Search variants

The JCR repository has an integrated lucene search that can be used to search for words in a query and rate the
results.

- The top N results can either be used directly, or
- their embeddings can be compared with the query embeddings to filter the best out, or
- the LLM can rate them directly.

### Search trigger variants

- The search can be triggered directly from the users query, or
- the LLM can be asked to preprocess it, or
- in a chat the LLM can trigger search actions on its own when it sees that it needs more information (or always after a
  user message).

### Generated content variants

- Rating of links for a search result
- Answer with related links
- Just an answer, supported by a search

## More ideas for later

- Generate recommendations for related content for a page, either at editing time or on the fly
- Generate links from a page for the CMS editor, to generate teasers or links to related content
- Find related content to support the CMS editor in creating a page: additional source for content creation dialog?
- Possibly: find related assets (embedding of description?)
- ??? "Real time content adaption" : create pages about something on the fly? "View" according to a given topic?
- Content analysis: clustering of pages by content, through embeddings
- Personalized Content Recommendations based on interaction history, using embeddings for finding related content.
- clustering of user feedback (comments)?
- adaption of search results by integrating user comments
- ??? custom content feed for a user, based on previous interactions and description of intent

PROMPT: create 20 more variants how RAG can support either the end user or the content creator. Describe the features
from the users / content creators perspective, not the implementation. 

# Prototypes

We have implemented several prototypes for advanced functionalities that are already useable, but haven't gotten the
care that would be advisable for production use. That is, they might not yet be well integrated into the UI yet, or
they would need some more optimization for production usage.
But you are welcome to try them out, even now they might just be what you need.
Please make sure to give us feedback on what you think about them and what you would like to see improved!

## AI supported search / RAG assisted question answering

This is a prototype how to support search with AI techniques like embeddings and how to support question answering with
RAG (retrieval augmented generation) techniques. It provides a servlet (RAGServlet at /bin/cpm/ai/rag) and two mini
applications that let you access the servlet for demonstration purposes. There is not yet an actual component that
uses the search since the way it is used is likely quite project specific and might be implemented in Javascript,
anyway, so that the servlet could be used as a backend.

- a search that searches for all words that are in the query and rates the top n results using AI embeddings before
  displaying them
- a RAG supported query answering (no chat) that does this kind of search and then provides the AI with the texts of the
  top 5 found pages for answering the query.

Optionally, both use 
[hybrid retrieval](https://generativeai.pub/advanced-rag-retrieval-strategies-hybrid-retrieval-997d39659720)
having the AI generate keywords from the query and using them for retrieval, and combining the results of that with the 
retrieval based on the queries words itself.

The mini applications are available at the following URLs (for a local installation):

- Composum: http://localhost:9090/libs/composum/pages/options/ai/prototype.html
- AEM: http://localhost:4502/apps/composum-ai/prototype.html

Precondition: there has to be a lucene fulltext index for the searched repository parts. That is already there in
Composum Pages (/oak:index/lucene) but possibly has to be set up at AEM - e.g. with:
https://github.com/ist-dresden/composum-AI/blob/develop/backend/slingbase/src/test/resources/lucene/aemContentTextLucene/.content.xml

More discussion / ideas are in
https://github.com/ist-dresden/composum-AI/blob/develop/featurespecs/9RAGvariants.md

## AI Page Templating : AI supported page creation in one step

While the content creation dialog can support you when working on individual texts in a page, you might sometimes
want support for creating whole pages at once. That's the idea for page templating: you create a template page that
contains prompts (instructions) to create the individual texts in the page. For creating an actual page from that
template you copy it and supply the AI with text(s) from which it can gather the information you want to present on the 
page (currently either in form of one or more URL(s) or a text).
Since it'll likely take some work to get the prompts right, that will likely only be useful if you have quite a
number of very similar pages to create about different topics / products etc.

For more details see the [AI Page Templating](aiPageTemplating.md).

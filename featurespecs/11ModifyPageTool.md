# Page Modification Tool

## Basic idea

We want the Sidebar AI to be able to modify pages. For a start we implement a feature that allows the user to modify
the existing properties of a page.

There are two necessary operations for the tool:

- read all properties of a page, incl. path to the component, property name and value
- write one or several properties of a page

For the result of the read operation and as parameters for the write operation we can use the same kind of JSON object:

```
{
  "components": [
    {
      "componentPath": "/content/mysite/en/page",
      "sling:resourceType": "mysite/components/page",
      "componenttitle": "Title of the component",
      "properties": {
        "jcr:title": "My Page",
        "jcr:description": "This is a page"
      }
    }
  ]
}
```

The componenttitle is the jcr:title of the component. For the write operation we throw an error if
sling:resourceType and componenttitle are changed - they are optional. Of course there can be many components in the
array. Only properties that have at least two separate whitespace (find regex "\\s\\S+\\s+") are returned, and can
be written. This collects recursively all components of a page - the page itself and all its children.

## Implementation remarks

Since AITool allows only one function, we need two tools: ModifyPageReadTool and ModifyPageWriteTool. Both can be
enabled / disabled separately.

## Open Points

How about JCR nodes in a page that do not have a sling:resoureType?

## More ideas

It would be nice if the tool could create new components, determine which properties a component dialog has,
move components around, etc. But that's hard to do in a generic way. So we start with a simple implementation that
just allows to modify the texts.

The tool should be consistent with the ApproximateMarkdownServiceImpl, but that's too much effort for a quick POC.

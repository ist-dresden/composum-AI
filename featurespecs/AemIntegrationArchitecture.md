# Architecture of the AEM integration of Composum AI

This records the general decisions about the AEM integration - see [Ideas for AEM Integration](AEMIntegrationIdeas.md)
for the discussion of various ideas and variants.

## Dialog rendering

We use the AEM standard way as far as possible. That means we use for a pop up dialog like the Content Creation
Dialog a TouchUI Dialog (Coral 3). It will be triggered from Javascript, though, so it can likely be a static URL.
Since we need a place to store the prompts, anyway, we render it at a resource at
/conf/composum-ai/settings/dialogs that will have subnodes with the prompt information. This way we could create 
several configurations for different sites later.

http://localhost:4502/mnt/override/apps/composum-ai/components/contentcreation/_cq_dialog.html/conf/composum-ai/settings/dialogs/contentcreation

## Javascript structure

We will conform to the Javascript handling used AEM archetype and use ES6 modules for AEM specific code. If it 
happens that some of the code could be used by both the Composum and AEM variant, we'll use the IIFE pattern used in 
Composum, since ES6 module support wouldn't be easy to use in Composum as it'd need a separate frontend build since 
the modules would have to be packed for use with Composum client libraries.

## Structure of responses

To make things compatible with the Composum Pages variant of Composum AI: if we generate a JSON response we generate 
it in the same way as in Composum: e.g.
{
    "status": 200,
    "success": true,
    "warning": false,
    "data": {
        "result": {
            "streamid": "7190c15e-bd98-4ceb-bd45-5e800b6370f0"
        }
    }
}
or in the error case:
{
  "status": 400,
  "success": false,
  "warning": false,
  "title": "Error",
  "messages": [
    {
      "level": "error",
      "text": "error",
      "rawText": "error",
      "arguments": [
        "arg1",
        "arg2"
      ],
      "timestamp": <timestamp>
    }
  ]
}

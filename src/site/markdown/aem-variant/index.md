# Composum AI for Adobe AEM 6.5 and AEMaaCS

The AEM variant of the [Composum AI](../index.md) supports both AEM 6.5.x and AEM as a Cloud Service. It currently
provides two assistant dialogs with different focus and capabilities:

- the Content Creation Assistant geared to text content generation for direct use in the editor, and
- the Side Panel AI that is geared for analysing and discussing the content of the page or components with the AI, and
  provides a chat like interface.
- workflows and rollout configurations for [automatic transparent translation of live copies](automaticTranslation.md).

Both of them offer a library with various prompt examples that can be used as is or
modified into
processing / analysis instructions to process page parts or the whole page text, or external texts into summaries,
introductions, conclusions, rewrite texts, generate suggestions for extending texts, phrases, headlines and so forth.
[Our blog](https://www.composum.com/home/blog/AEM/composumAI-AEM.html) gives a nice quick overview over the
functionality. The [usage documentation](usage.md) is more detailed.
The [installation instructions](installation.md) tell you how to try it out and / or install it in your server. The
Sling package is available freely over the public maven repository. The project
is [open source](https://github.com/ist-dresden/composum-AI) and freely available
under a MIT license.

<div style="display: flex; flex-wrap: wrap; justify-content: space-between; margin-top: 2em;">
  <div style="flex: 0 0; margin-bottom: 2em; text-align: center;">
    <iframe src="https://www.youtube.com/embed/96gv-F4zX_o?si=eOXepyR2uP-tPjXp" 
        title="Living Slideshow for the Composum AI for Adobe AEM" frameborder="0" width="560" height="315" 
        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
        allowfullscreen="allowfullscreen"></iframe>
    <p>Living Slideshow for the Composum AI for Adobe AEM</p>
  </div>

  <div style="flex: 0 0; margin-bottom: 2em; text-align: center;">
    <iframe src="https://www.youtube.com/embed/SgZ9OH2aMDI?si=vHKaGX4I_isyBREg" 
          title="The Composum AI for AEM: generate / rewrite / analyze texts with ChatGPT" 
          frameborder="0" width="560" height="315" 
          allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
          allowfullscreen="allowfullscreen"></iframe>
    <p>Living Slideshow for the Composum AI for the Composum Pages CMS</p>
  </div>
</div>

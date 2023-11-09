# Installation of the Composum AI within Composum Pages

## Just try it out

The easiest way to try it is the [Composum Cloud](https://cloud.composum.com) where Composum-AI is deployed. You can
get a free account there, create a site and test it.

Second there is
the [Composum Starter](https://search.maven.org/artifact/com.composum.platform.features/composum-launcher-feature-composumstarter/) (
a JAR you can execute locally) or the docker image
[composum/featurelauncher-composum](https://hub.docker.com/r/composum/featurelauncher-composum)
available through the
[Composum Launcher](https://github.com/ist-dresden/composum-launch) project - see there for description.

You will need an [OpenAI API key](https://platform.openai.com/account/api-keys) secret key to run it, and configure
that in the [Felix Console configuration tab](http://localhost:8080/system/console/configMgr) at "GPT Chat
Completion Service".

## Installation in Composum Pages

Composum AI is available as
[a Sling package](https://central.sonatype.com/artifact/com.composum.ai/composum-ai-integration-composum-package/)
that can be installed via package manager or via Sling features.

Alternatively Composum AI is packaged in the
[Composum Starter](https://search.maven.org/artifact/com.composum.platform.
features/composum-launcher-feature-composumstarter/)
and the docker image [composum/featurelauncher-composum](https://hub.docker.com/r/composum/featurelauncher-composum) ,
both created in the [Composum Launcher](https://github.com/ist-dresden/composum-launch) project.

The release notes are available
[on Github](https://github.com/ist-dresden/composum-AI/releases).

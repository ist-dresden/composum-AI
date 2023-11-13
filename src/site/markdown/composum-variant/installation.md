# Installation of the Composum AI within Composum Pages

## Just try it out

The easiest way to try it is the [Composum Cloud](https://cloud.composum.com) where Composum-AI is deployed. You can
get a free account there, create a site and test it.

Second, there is
the [Composum Starter](https://search.maven.org/artifact/com.composum.platform.features/composum-launcher-feature-composumstarter/)
(a JAR you can execute locally) or the docker image
[composum/featurelauncher-composum](https://hub.docker.com/r/composum/featurelauncher-composum)
available through the
[Composum Launcher](https://github.com/ist-dresden/composum-launch) project - see there for description.

You will need an [OpenAI API key](https://platform.openai.com/account/api-keys) secret key to run it, which can
either be put into an environment variable `OPENAI_API_KEY` or be configured via OSGI configuration in the Felix
console [/system/console/configMgr](http://localhost:8080/system/console/configMgr) at "Composum AI OpenAI
Configuration"
(or "Composum AI GPT Chat Completion Service", depending on the version) - see the
[configuration instructions](configuration.md) for details and other configuration options.

## Installation in Composum Pages

Composum AI is available as
[a Sling package](https://central.sonatype.com/artifact/com.composum.ai/composum-ai-integration-composum-package/)
that can be installed via package manager or via Sling features. For more details on Composum Pages setup see the
[setup instructions](https://www.composum.com/home/pages/setup.html).

Alternatively, Composum AI is prepackaged in the
[Composum Starter](https://search.maven.org/artifact/com.composum.platform.
features/composum-launcher-feature-composumstarter/)
and the docker image [composum/featurelauncher-composum](https://hub.docker.com/r/composum/featurelauncher-composum) ,
both created in the [Composum Launcher](https://github.com/ist-dresden/composum-launch) project.

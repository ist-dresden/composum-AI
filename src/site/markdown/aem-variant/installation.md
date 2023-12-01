# Installation of the Composum AI within Adobe AEM

For AEM as a cloud service or AEM 6.5, you have to add the
[package on maven central](https://central.sonatype.com/artifact/com.composum.ai.aem/composum-ai.all) to your
deployment or [download](https://repo1.maven.org/maven2/com/composum/ai/aem/composum-ai.ui.apps/)
and deploy this package via package manager. It should only be deployed on the author server -
there are currently only services that support the editors.
The release notes are available
[on Github](https://github.com/ist-dresden/composum-AI/releases).

For the access to the ChatGPT backend an [OpenAI API Key](https://platform.openai.com/api-keys)
is necessary. If you're trying out Composum AI locally on a development server or a AEM 6.5 standard installation,
it might be easiest to put it into an environment variable `OPENAI_API_KEY` or configure it via
OSGI in the "Composum AI GPT Chat Completion Service" / "Composum AI OpenAI Configuration" in the Felix console.
For a testing or production environment there are also several other ways to configure the key -
compare the [configuration instructions](configuration.md). If you're just trying it out on a test system and want
it to be available for everybody it is sufficient to put it into a
[secret environment-specific configuration](https://experienceleague.adobe.com/docs/experience-manager-cloud-service/content/implementing/using-cloud-manager/environment-variables.html?lang=en)
with name OPENAI_API_KEY .

BTW: If you'd like to give it a quick try without installing it, you might also try the
[Composum Pages variant](../composum-variant/) on our [Composum Cloud](https://cloud.composum.com),
which is freely available for testing purposes. The functionality of the Sidebar AI and the Content Creation Dialog
work very similar to the AEM pendants, though the general usage of Composum Pages is different than AEM, of course.

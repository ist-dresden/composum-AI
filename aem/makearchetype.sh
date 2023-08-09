# recreates the archetype as it's been used for this.

# see https://experienceleague.adobe.com/docs/experience-manager-core-components/using/developing/archetype/using.html?lang=en
# see https://github.com/adobe/aem-project-archetype/blob/master/README.md

mvn -o -B archetype:generate  -D archetypeGroupId=com.adobe.aem  -D archetypeArtifactId=aem-project-archetype  -D archetypeVersion=37  -D aemVersion=cloud  -D appTitle="Composum AI"  -D appId="composum-ai"  -D groupId="com.composum.ai.aem" -D artifactId="composum-ai" -D includeExamples=n -D frontendModule=general -D version=0.4.2-SNAPSHOT -D includeDispatcherConfig=n -D singleCountry=y -D sdkVersion="2023.7.12874.20230726T072051Z-230702"

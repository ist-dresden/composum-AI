# Internal remarks about preparing a release

## Checklist

- Run release workflow in github actions
- wait > 30min until release is
  on https://repo1.maven.org/maven2/com/composum/ai/composum-ai-integration-composum-package/
- update composum.ai.version in composum-launch
- locally build composum-launch
- create release in composum-launch incl. docker images - composum-launch/RELEASING.md
    - wait for release to appear
      at https://mvnrepository.com/artifact/com.composum.platform.features/composum-launcher-feature-composumstarter
- check on test.composum.com
- deploy on cloud.composum.com
- check there


## Local testing of releasing

Starting with parent release 1.7, the release creation is meant to be done automatically in github actions. There are two kinds of workflow: in Nodes at .github/workflows/createrelease.yml (meant to be copied to all the other Github projects) it builds the whole tree in the project, but for composum-meta .github/workflows/createrelease.yml does permit to select a subdirectory, as there are several trees (ist/parent and assembly) with different release schedules.
To check release creation locally in a dry run, you can use the following command lines:
mvn clean release:clean
mvn -B -ntp -P nexus-staging release:prepare -DdryRun=true -DpushChanges=false
mvn -B -ntp -P nexus-staging release:perform -DdryRun=true -DlocalCheckout=true -DdeployAtEnd=true
(The latter doesn't do much, though.)
To create a release locally for testing purposes, you can use the following command lines:
mvn -B -ntp -s .github/settings-istrepo.xml release:prepare -DpushChanges=false
mvn -B -ntp -P nexus-staging release:perform -DlocalCheckout=true -DdeployAtEnd=true "-Dgoals=clean install package source:jar javadoc:jar install"
This does not push the release commits to upstream, and just does an install instead of deploy at the end, so nothing is uploaded. The commits can be rolled back with mvn release:rollback release:clean ; you'll want to reset the git HEAD later, though.

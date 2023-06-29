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

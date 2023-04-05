# Architecture description

This file documents the architecture of this application and records implementation decisions that were taken and 
their reasons.

## Used libraries

There is a list of [community libraries](https://platform.openai.com/docs/libraries/community-libraries) that has
a [openai-java](https://github.com/TheoKanning/openai-java) library. It has several parts - an API module
containing model classes with appropriate JSON annotations for the calls, which is handy, and a service
implementation that has quite a lot of dependencies we would need to deploy. The service module would likely
introduce more complications than it's worth, so we just take the API module - Jackson and HttpClient 4 are 
deployed on Composum systems, anyway.

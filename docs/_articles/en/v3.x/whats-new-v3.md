---
title: What's new in v3.x?
lang: en
---

## What’s new in v3.x?

### Access to Salesforce Graph Engine
We've now added our quickly evolving [Salesforce Graph Engine](./en/v3.x/architecture/sfg-engine/) as one of the backend engines of SFCA. Preview its features using the newly added [`scanner:run:dfa`](./en/v3.x/scanner-commands/dfa/) command. You can also [try it out on our example app](./en/v3.x/salesforce-graph-engine/try-it-yourself/).

### Upgraded major versions
As a starting point, we have upgraded major versions of the eslint family of engines:
1. eslint from major version 6 to major version 8
2. typescript-eslint plugin from major version 2 to major version 5
3. eslint-lwc plugin from major version 0 to major version 1

### RetireJS is now a default engine
Another change is that RetireJS will be included as a default engine along with PMD, Eslint, and Eslint-Typescript.

### Cleaned up deprecated flags
`-v/--violations-cause-error` flag on `scanner:run` has been replaced by `-s/--severity-threshold`. 

There are other exciting changes in the works that will be released as a part of minor releases. Watch out this space for more information!


## Installing version v3.x
Follow [these](./en/v3.x/getting-started/install) steps to install and try out the latest features in {{ site.data.versions-v3.scanner }}.
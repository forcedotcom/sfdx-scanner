---
title: What's new in v3.x?
lang: en
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/code-analyzer-3x.html
---

## What’s new in v3.x

### Added Salesforce Graph Engine
[Salesforce Graph Engine](./en/v3.x/salesforce-graph-engine/introduction/) is a new Code Analyzer engine. Preview its features using the newly added [`scanner run dfa`](./en/v3.x/scanner-commands/dfa/) command, and check out our [example app](./en/v3.x/salesforce-graph-engine/try-it-yourself/).

### Upgraded major versions
We upgraded major versions of ESLint engines.

* `eslint` from major version 6 to major version 8
* `typescript-eslint` plug-in from major version 2 to major version 5
* `eslint-lwc` plug-in from major version 0 to major version 1

### Included RetireJS as a default engine
RetireJS is a default engine along with PMD, ESLint, and ESLint-Typescript. To provide more information about RetireJS violations, we enhanced the `scanner run` command with a new `--verbose-violations` flag.

### Cleaned up deprecated flags
We replaced the  `-v/--violations-cause-error` flag on `scanner run` with the `-s/--severity-threshold`.

## Installing version v3.x
Follow [these](./en/v3.x/getting-started/install) steps to install the latest features in version {{ site.data.versions-v3.scanner }}.

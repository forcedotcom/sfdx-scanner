---
title: 'ESLint'
lang: en
---
## What is ESLint?
[ESLint](https://eslint.org) is a popular linting tool for JavaScript. It provides numerous static analysis rules that
help developers write quality code.

## How does Salesforce CLI Scanner use ESLint?
Salesforce CLI Scanner's ```scanner:run``` command uses ESLint to scan targeted JavaScript files. By default, all rules
are executed, but one may change which rules are evaluated through the use of the flags described in the
[command's documentation](./en/scanner-commands/run/#options).

Additionally, ESLint is highly configurable through plugins, and Salesforce CLI Scanner provides support for the following:
- [TypeScript-ESLint](https://github.com/typescript-eslint/typescript-eslint), which adds support for
[TypeScript](https://typescriptlang.org), a strongly-typed superset of JavaScript. By default, this plugin will be used
to evaluate any targeted TypeScript (```.ts```) files.
- [ESLint-LWC](https://github.com/salesforce/eslint-plugin-lwc), the official ESLint rules for Salesforce Lightning Web
Components. These rules are not enabled by default, and can be enabled on a per-run basis with the
```--engine "eslint-lwc"``` flag, or on a permanent basis by modifying the `Config.json` file directly.

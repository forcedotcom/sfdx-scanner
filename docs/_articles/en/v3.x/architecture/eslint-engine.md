---
title: 'ESLint'
lang: en
redirect_from: /en/architecture/eslint-engine
---
## What is ESLint?
ESLint is a popular linting tool for JavaScript. It provides numerous static analysis rules that help developers write quality code.

## How does Salesforce Code Analyzer use ESLint?
The Salesforce Code Analyzer (Code Analyzer) ```scanner run``` uses ESLint to scan targeted JavaScript files. By default, all rules are executed, but you can change which rules are evaluated through Code Analyzer optional flags described in the Code Analyzer Command Reference.

Additionally, ESLint is highly configurable through plug-ins. Code Analyzer supports these plug-ins.

- [TypeScript-ESLint](https://github.com/typescript-eslint/typescript-eslint) adds support for [TypeScript](https://typescriptlang.org), a strongly typed superset of JavaScript. By default, this plug-in is used to evaluate any targeted TypeScript (```.ts```) files.
- [ESLint-LWC](https://github.com/salesforce/eslint-plugin-lwc), the official ESLint rules for Salesforce Lightning Web Components (LWC). ESLint rules aren’t enabled by default. They can be enabled on a per-run basis with the ```--engine eslint-lwc``` flag or on a permanent basis by modifying the ```config.json``` file directly.

## See Also

- [ESLint](https://eslint.org/)
- [Salesforce Code Analyzer Command Reference](./en/v3.x/scanner-commands/run/#options)

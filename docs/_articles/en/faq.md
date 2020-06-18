---
title: 'Frequently Asked Questions'
lang: en
---

## Table of Contents

#### [Questions about `sfdx-scanner`](#questions-about--sfdx-scanner-)
  * [What is `sfdx-scanner`?](#q--what-is--sfdx-scanner--)
  * [Is `sfdx-scanner` part of the App Exchange security review process?](#q--is--sfdx-scanner--part-of-the-app-exchange-security-review-process-)
  * [Is `sfdx-scanner` only for Salesforce CLI (`sfdx`) projects?](#q--is--sfdx-scanner--only-for-salesforce-cli---sfdx---projects-)
#### [Questions about language support](#questions-about-language-support)
  * [What languages does `sfdx-scanner` support?](#q--what-languages-does--sfdx-scanner--support-)
  * [How do I add a language to `sfdx-scanner`?](#q--how-do-i-add-a-language-to--sfdx-scanner--)
  *  [How do I remove a language from `sfdx-scanner`?](#q--how-do-i-remove-a-language-from--sfdx-scanner--)
#### [Questions about adding and removing rules](#questions-about-adding-and-removing-rules)
  * [How do I add new rules for Language X?](#q--how-do-i-add-new-rules-for-language-x-)
#### [Questions about dependencies and setup](#questions-about-dependencies-and-setup)
  * [What else do I need before I can use `sfdx-scanner`?](#q--what-else-do-i-need-before-i-can-use--sfdx-scanner--)


## Questions about `sfdx-scanner`

#### Q: What is `sfdx-scanner`?
A: `sfdx-scanner` is a [Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm) plug-in that helps developers write better and more
secure code.

The plug-in uses multiple code analysis engines, including PMD and ESLint, to inspect your code. It identifies potential problems, from inconsistent naming to security vulnerabilities, and conveys these problems with easy-to-understand results.
You can run the scanner on-command in the CLI, or integrate it into your CI/CD framework so you can run it against every code change.

#### Q: Is `sfdx-scanner` part of the App Exchange security review process?
A: `sfdx-scanner` is separate from the App Exchange security review process, but it enforces many of the same rules. Because it can be executed at-will and provides results in minutes, it lets you find and fix problems faster. As a result, you
can be more confident in the code you submit for security review.

#### Q: Is `sfdx-scanner` only for Salesforce CLI (`sfdx`) projects?
A: No! `sfdx-scanner` can be used on any codebase.

## Questions about language support

#### Q: What languages does `sfdx-scanner` support?
A: By default, `sfdx-scanner` supports code written in Apex, VisualForce, Java, JavaScript, and TypeScript. You can extend it to support any language.

#### Q: How do I add a language to `sfdx-scanner`?
A: The file types targeted by each rule engine are defined in `~/.sfdx-scanner/Config.json` in the `targetPatterns` property for each entry.

To make a particular rule engine scan a new language, add that language's file extension to the `targetPatterns` property for that rule engine in `~/.sfdx-scanner/Config.json`. For example, to start scanning Python files with PMD, add `**/*.py` to the `targetPatterns` property for PMD.

Updating the `~/.sfdx-scanner/Config.json` file doesn't add any rules against that language. If you want to run rules against the new language, write them yourself and add them with the `scanner:rule:add` command.

#### Q: How do I remove a language from `sfdx-scanner`?
A: Remove the language's file extensions from all `targetPatterns` properties in `~/.sfdx-scanner/Config.json`. The `sfdx-scanner` plug-in then ignores files of that type.

Removing this information from the `~/.sfdx-scanner/Config.json`file doesn't remove existing custom rules from the registry. To remove existing custom rules, run the `scanner:rule:remove` command.

## Questions about adding and removing rules

#### Q: How do I add new rules for Language X?
A: Currently, you can add custom rules for only __PMD__. Bundle these rules into a JAR, then add the JAR to the rule registry with the `scanner:rule:add` command.

If the language is not already supported, follow the steps in ["How do I add a new language to `sfdx-scanner`?"](/en/faq/#q-how-do-i-add-a-new-language-to-sfdx-scanner).

## Questions about dependencies and setup

#### Q: What else do I need before I can use `sfdx-scanner`?
A: You must:
- Install the [Salesforce CLI](https://developer.salesforce.com/tools/sfdxcli) on your computer.
- Use Java v1.8 or later.

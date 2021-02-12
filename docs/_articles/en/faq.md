---
title: 'Frequently Asked Questions'
lang: en
---
## Questions about `Salesforce CLI Scanner`

#### Q: What is `Salesforce CLI Scanner`?
A: `Salesforce CLI Scanner` is a [Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm) plug-in that helps developers write better and more
secure code.

The plug-in uses multiple code analysis engines, including PMD, ESLint, and RetireJS to inspect your code. It identifies potential problems, from inconsistent naming to security vulnerabilities, and conveys these problems with easy-to-understand results.
You can run the scanner on-command in the CLI, or integrate it into your CI/CD framework so you can run it against every code change.

#### Q: Is `Salesforce CLI Scanner` part of the App Exchange security review process?
A: `Salesforce CLI Scanner` is separate from the App Exchange security review process, but it enforces many of the same rules. Because it can be executed at-will and provides results in minutes, it lets you find and fix problems faster. As a result, you
can be more confident in the code you submit for security review.

#### Q: Is `Salesforce CLI Scanner` only for Salesforce CLI (`sfdx`) projects?
A: No! `Salesforce CLI Scanner` can be used on any codebase.

## Questions about language support

#### Q: What languages does `Salesforce CLI Scanner` support?
A: By default, `Salesforce CLI Scanner` supports code written in Apex, VisualForce, Java, JavaScript, and TypeScript. You can extend it to support any language.

#### Q: How do I add a language to `Salesforce CLI Scanner`?
A: The file types targeted by each rule engine are defined in `~/.sfdx-scanner/Config.json` in the `targetPatterns` property for each entry.

To make a particular rule engine scan a new language, add that language's file extension to the `targetPatterns` property for that rule engine in `~/.sfdx-scanner/Config.json`. For example, to start scanning Python files with PMD, add `**/*.py` to the `targetPatterns` property for PMD.

Updating the `~/.sfdx-scanner/Config.json` file doesn't add any rules against that language. If you want to run rules against the new language, write them yourself and add them with the `scanner:rule:add` command.

#### Q: How do I remove a language from `Salesforce CLI Scanner`?
A: Remove the language's file extensions from all `targetPatterns` properties in `~/.sfdx-scanner/Config.json`. The `Salesforce CLI Scanner` plug-in then ignores files of that type.

Removing this information from the `~/.sfdx-scanner/Config.json`file doesn't remove existing custom rules from the registry. To remove existing custom rules, run the `scanner:rule:remove` command.

## Questions about adding and removing rules

#### Q: How do I enable Engine X's default rules for Language Y?
A: That depends on the engine in question.
- __PMD__: Add the language's name to the PMD's `supportedLanguages` array in
`~/.sfdx-scanner/Config.json`.
<br/>
If the language is not already supported, you must additionally follow the steps outlined above in
["How do I add a new language to `Salesforce CLI Scanner`?"](./en/faq/#q-how-do-i-add-a-new-language-to-sfdx-scanner).

#### Q: How do I add new rules for Language X?
A: Currently, you can add custom rules for only __PMD__. Bundle these rules into a JAR, then add the JAR to the rule registry with the [`scanner:rule:add`](./en/scanner-commands/add/#example) command.

If the language is not already supported, follow the steps in "How do I add a new language to `Salesforce CLI Scanner`?"

## Questions about dependencies and setup

#### Q: What else do I need before I can use `Salesforce CLI Scanner`?
A: You must:
- Install the [Salesforce CLI](https://developer.salesforce.com/tools/sfdxcli) on your computer.
- Use Java v1.8 or later.

#### Q: How do I update the Salesforce CLI Scanner?
A: You must:
- You can update the plugin to the latest version by following the instructions listed [here](./en/getting-started/install/#upgrade-plug-in)
- To update to a specific version of the plugin, here is the example `sfdx plugins:install @salesforce/sfdx-scanner@2.3.0`

#### Q: How can I use `Salesforce CLI Scanner` in my CI/CD?
A: You can use the `sfdx scanner:run` command in any scripts used by your CI/CD. You'll also probably want to do the following:
- Use the `-o/--outfile` flag to write your results to a file, so you'll have an artifact of the results.
- Use the `-v/--violations-cause-error` flag so violations cause a non-zero exit code, since many CI/CD frameworks care about such things.

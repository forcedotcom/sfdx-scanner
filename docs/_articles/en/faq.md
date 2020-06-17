---
title: 'Frequently Asked Questions'
lang: en
---

## Table of Contents

#### [Questions about `sfdx-scanner`](#questions-about-sfdx-scanner-1)
- [What is `sfdx-scanner`?](#q-what-is-sfdx-scanner)
- [Is `sfdx-scanner` part of the App Exchange security review process?](#q-is-sfdx-scanner-part-of-the-app-exchange-security-review-process)
- [Is `sfdx-scanner` _only_ for sfdx projects?](#q-is-sfdx-scanner-only-for-sfdx-projects)

#### [Questions about language support](#questions-about-language-support-1)
- [What languages does `sfdx-scanner` support?](#q-what-languages-does-sfdx-scanner-support)
- [How do I _add_ a new language to `sfdx-scanner`?](#q-how-do-i-add-a-new-language-to-sfdx-scanner)
- [How do I _remove_ a language from `sfdx-scanner`?](#q-how-do-i-remove-a-language-from-sfdx-scanner)

#### [Questions about adding/removing rules](#questions-about-addingremoving-rules-1)
- [How do I add new rules for Language X?](#q-how-do-i-add-new-rules-for-language-x)

#### [Questions about dependencies/setup](#questions-about-dependenciessetup-1)
- [What else do I need before I can use `sfdx-scanner`?](#q-what-else-do-i-need-before-i-can-use-sfdx-scanner)

## Questions about `sfdx-scanner`

#### Q: What is `sfdx-scanner`?
A: `sfdx-scanner` is an `sfdx` plugin that helps developers write better and more
secure code.
<br/>
It uses multiple code analysis engines including PMD and ESLint to inspect your
code, identifying potential problems ranging from inconsistent naming to security
vulnerabilities, and conveying these problems with easy-to-understand results.
You can run the scanner on-command in the CLI, or integrate it into your CI/CD
so you can run it against every change.
#### Q: Is `sfdx-scanner` part of the App Exchange security review process?
A: `sfdx-scanner` is separate from the App Exchange security review process,
but it enforces many of the same rules. Since it can be executed at-will and
provides results in minutes, it lets you find and fix problems faster, so you
can be more confident in the code you submit for security review.
#### Q: Is `sfdx-scanner` _only_ for `sfdx` projects?
A: Absolutely not! `sfdx-scanner` can be used on _any_ codebase!
## Questions about language support
#### Q: What languages does `sfdx-scanner` support?
A: By default, `sfdx-scanner` supports code written in Apex, VisualForce, Java,
JavaScript, and TypeScript.<br/>
However, it can be extended to support _any_ language.
#### Q: How do I _add_ a new language to `sfdx-scanner`?
A: The file types targeted by each engine are defined in `~/.sfdx-scanner/Config.json`,
in the `targetPatterns` property for each entry.
<br/>
To make a particular engine scan a new language, add that language's file extension to
the `targetPatterns` property for that engine in `~/.sfdx-scanner/Config.json`. E,g.,
to start scanning Python files with PMD, add `**/*.py` to its `targetPatterns`.
<br/>
Note that this _does not_ add any rules against that language. If you want to run
rules against the new language, you'll need to write them yourself and add them with
`scanner:rule:add`.
#### Q: How do I _remove_ a language from `sfdx-scanner`?
A: Removing the language's file extensions from all `targetPatterns` properties
in `~/.sfdx-scanner/Config.json` will cause `sfdx-scanner` to ignore files of
that type.
<br/>
Note that this _does not_ remove existing custom rules from the registry. That
can be done with the `scanner:rule:remove` command.
## Questions about adding/removing rules
#### Q: How do I add new rules for Language X?
A: Currently, only custom rules for __PMD__ may be added. These rules should be
bundled into a JAR, and that JAR should be added to the rule registry using the
`scanner:rule:add` command.
<br/>
If the language is not already supported, you must additionally follow the steps
outlined in ["How do I add a new language to `sfdx-scanner`?"](#q-how-do-i-_remove_-a-language-from-sfdx-scanner).
## Questions about dependencies/setup
#### Q: What else do I need before I can use `sfdx-scanner`?
A: You need the following:
- [SFDX CLI](https://developer.salesforce.com/tools/sfdxcli)
- Java v1.8 or later

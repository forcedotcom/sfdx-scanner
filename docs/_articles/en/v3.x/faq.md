---
title: 'Frequently Asked Questions'
lang: en
---
## Questions about `Salesforce Code Analyzer`

#### Q: What is `Salesforce Code Analyzer`?
A: `Salesforce Code Analyzer` is a [Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm) plug-in that helps developers write better and more
secure code.

The plug-in uses multiple code analysis engines, including PMD, ESLint, and RetireJS to inspect your code. It identifies potential problems, from inconsistent naming to security vulnerabilities, and conveys these problems with easy-to-understand results.
You can run the code analyzer on-command in the CLI, or integrate it into your CI/CD framework so you can run it against every code change.

#### Q: Is `Salesforce Code Analyzer` part of the App Exchange security review process?
A: `Salesforce Code Analyzer` is separate from the App Exchange security review process, but it enforces many of the same rules. Because it can be executed at-will and provides results in minutes, it lets you find and fix problems faster. As a result, you
can be more confident in the code you submit for security review.

#### Q: Is `Salesforce Code Analyzer` only for Salesforce CLI (`sfdx`) projects?
A: No! `Salesforce Code Analyzer` can be used on any codebase.

## Questions about language support

#### Q: What languages does `Salesforce Code Analyzer` support?
A: By default, `Salesforce Code Analyzer` supports code written in Apex, VisualForce, Java, JavaScript, XML, and TypeScript.

#### Q: How do I get support for additional languages?
Please create an Issue on our [Github repo](https://github.com/forcedotcom/sfdx-scanner). We'll try to help you out. 

## Questions about adding and removing rules

#### Q: How do I enable Engine X's default rules for Language Y?
A: That depends on the engine in question.
- __PMD__: Add the language's name to the PMD's `supportedLanguages` array in
`~/.sfdx-scanner/Config.json`.
<br/>
If the language is not already supported, please log an Issue as per
["How do I get support for additional languages?"](./en/v3.x/faq/#q-how-do-i-get-support-for-additional-languages).

#### Q: How do I add new rules for Language X?
A: Currently, you can add custom rules for only __PMD__. Ruleset files, and Category files defining only XPath-based rules, may be referenced as standalone XML files. Java-based rules must be bundled into a JAR. The JAR/XML can then be added to the rule registry with the ```scanner:rule:add``` command.

If the language is not already supported, follow the steps in "How do I add a new supported language to `Salesforce Code Analyzer`?"

## Questions about dependencies and setup

#### Q: What else do I need before I can use `Salesforce Code Analyzer`?
A: You must:
- Install the [Salesforce CLI](https://developer.salesforce.com/tools/sfdxcli) on your computer.
- Use Java v1.8 or later.

#### Q: How do I update the Salesforce Code Analyzer?
A: You must:
- Update the plugin to the latest version by following the instructions listed [here](./en/v3.x/getting-started/install/#upgrade-plug-in)
- To update to a specific version of the plugin, here is the example `sfdx plugins:install @salesforce/sfdx-scanner@{{ site.data.versions-v3.scanner }}`

#### Q: How can I use `Salesforce Code Analyzer` in my CI/CD?
A: You can use the `sfdx scanner:run` command in any scripts used by your CI/CD. You'll also probably want to do the following:
- Use the `-o/--outfile` flag to write your results to a file, so you'll have an artifact of the results.
- Use the -s/--severity-threshold flag to cause a non-zero exit code if any violations meet or exceed the provided value, since many CI/CD frameworks care about such things.

## Questions about Severity Threshold and Normalization

#### Q: How do I set a Severity Threshold for a code analyzer run?
A: When user runs the code analyzer with the `-s` or `--severity-threshold` flag and a threshold value, the code analyzer throws an error if violations are found with equal or greater severity than provided value. Values are 1 (high), 2 (moderate), and 3 (low). Exit code is the severity of the most severe violation(s). Using this flag also implicitly invokes the --normalize-severity flag.

#### Q: How do I get normalized severity?
A: PMD, ESLint & RetireJS all have different scales for reporting the Severity of the violations. When the user runs the code analyzer with the `--normalize-severity` flag, the `Salesforce Code Analyzer` will normalize the severity of violations across all invoked engines.  A normalized severity 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity. For the html output format, the normalized severity is displayed instead of the engine severity	

#### Q: How is the Severity normalized across all the engines?
A: Following table shows how the severity across all engines are normalized. 

| Normalized Severity | PMD     | ESLint | ESLint-LWC | ESLint-TypeScript | Retire-JS |
| ------------------- | ------- | ------ | ---------- | ----------------- | --------- |
| 1 (High)            | 1       | 2      | 2          | 2                 | 1         |
| 2 (Moderate)        | 2       | 1      | 1          | 1                 | 2         |
| 3 (Low)             | 3, 4, 5 |        |            |                   | 3

## Questions about CPD Engine

#### Q: What languages are supported by CPD in Code Analyzer?
A: To begin with, Code Analyzer supports Apex, Java, Visualforce, and XML in CPD.

#### Q: How do I know which files were not included by CPD execution?
A: Execute Code Analyzer for CPD with `--verbose` option. Files that were not handled by CPD will be listed under this message: `Path extensions for the following files will not be processed by CPD` Note that files are first filtered by `targetPatterns` provided in `~/.sfdx-scanner/Config.json` file.

#### Q: I have a file pattern for one of the supported languages that doesn’t get picked up by CPD. How do I add the file pattern?
A: As a first step, add your file pattern to the CPD engine’s `targetPatterns` in `~/.sfdx-scanner/Config.json`. If rerunning with the CPD engine option still doesn’t include the file, please create an issue for us and we’ll address it.

#### Q: In my violation messages from the CPD engine, I’m seeing multiple groups of the same checksum. The code fragment is also identical. Why aren’t these made the same group?
A: This is currently a [known issue](https://github.com/pmd/pmd/issues/2438) in CPD. We’ll address this with an internal fix in the future releases.

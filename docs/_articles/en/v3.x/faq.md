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
A: By default, `Salesforce Code Analyzer` supports code written in Apex, VisualForce, Java, JavaScript, XML, and TypeScript. You can specifically invoke `scanner:run` command with `--engine eslint-lwc` to get support for LWC.

#### Q: How do I get support for additional languages?
Please create an Issue on our [Github repo](https://github.com/forcedotcom/sfdx-scanner). We'll try to help you out. 

## Questions about adding and removing rules

#### Q: How do I enable Engine X's default rules for Language Y?
A: That depends on the engine in question.
- __PMD__: Add the language's name to the PMD's `supportedLanguages` array in
`~/.sfdx-scanner/{{ site.data.versions-v3.configfile }}`.
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
A: Execute Code Analyzer for CPD with `--verbose` option. Files that were not handled by CPD will be listed under this message: `Path extensions for the following files will not be processed by CPD` Note that files are first filtered by `targetPatterns` provided in `~/.sfdx-scanner/{{ site.data.versions-v3.configfile }}` file.

#### Q: I have a file pattern for one of the supported languages that doesn’t get picked up by CPD. How do I add the file pattern?
A: As a first step, add your file pattern to the CPD engine’s `targetPatterns` in `~/.sfdx-scanner/{{ site.data.versions-v3.configfile }}`. If rerunning with the CPD engine option still doesn’t include the file, please create an issue for us and we’ll address it.

#### Q: In my violation messages from the CPD engine, I’m seeing multiple groups of the same checksum. The code fragment is also identical. Why aren’t these made the same group?
A: This is currently a [known issue](https://github.com/pmd/pmd/issues/2438) in CPD. We’ll address this with an internal fix in the future releases.

## Questions about Salesforce Graph Engine

#### Q: Analyzing my code using `scanner:run:dfa` command takes much longer than all the other engines put together. Why is this?

Salesforce Graph Engine needs to build up a context of the source code in its entirety before applying rules to capture violations. Depending on the complexity of code, such as number of conditionals, classes to instantiate, method invocations, etc, some projects may take longer to process than others. You can control the number of threads or the timeout using [SFGE's environment variables](./en/v3.x/scanner-commands/dfa/#environment-variable-based-controls).


#### Q: My code is guarded through a different way than an Apex CRUD/FLS check. Is there a way to suppress violations on it?

Please read about [Engine Directives](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to see how this can be done.


## Questions about interpreting ApexFlsViolationRule results

#### Q: What do the violation messages mean?

Match your violation message with the different message formats described below to understand what it implies.

*Common Scenario*

>_Validation-Type_ validation is missing for _Operation-Name_ operation on _Object-Type_ with field(s) _Comma-Separated-Fields_

Parameter explanation:

* _Validation-Type_: Type of validation to be added. CRUD requires object-level checks. FLS requires field-level checks.

* _Operation-Name_: Data operation that needs to be sanitized.

* _Object-Type_: Object on which the data operations happen. If SFGE couldn’t guess the object type, you might see the variable name sometimes, and *SFGE_Unresolved_Argument* at other times.

* _Comma-Separated-Fields_: Fields on which the data operation works. If you see _Unknown_ as the only field or as one of the fields, this means SFGE did not have all the information to guess the fields, and trusts you to determine the unlisted fields.


*Additional Clause*


> _Validation-Type_ validation is missing for _Operation-Name_ operation on _Object-Type_ with field(s) _Comma-Separated-Fields_ - SFGE may not have parsed some objects/fields correctly. Please confirm if the objects/fields involved in these segments have FLS checks: _Unknown-Segments_

Same as the common scenario, but this additionally means SFGE is not confident about the object names and/or field names it detected. This could also happen if the field or object ends with __r. In both cases, please make sure the relational field/object or the unparsed segments has the required CRUD/FLS checks. Once you’ve taken care of it, you could add an [engine directive](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to let SFGE know that it doesn’t have to create a violation.

*stripInaccessible warning*

For stripInaccessible checks on READ operation, SFCA does not have the capability to verify that only sanitized data is used after the check. Please ensure that unsanitized data is discarded for _Object-Type_

This is thrown for all stripInaccessible checks on READ access type. This is because SFGE has no way to ensure that the sanitized value returned by SecurityDecision is indeed the value used in the code that follows the check. Once you’ve confirmed this, you can add an engine directive to ask SFGE to ignore this in the next run.

*Internal error*

Internal error. Work in progress. Please ignore.

This indicates that SFGE ran into an error while assessing the source/sink path mentioned in the violation. While we continue to work on fixing these errors, please make sure that the path in question is sanitized anyway.

#### Q: My data operation is already protected though not through a CRUD/FLS check. I'm confident that a CRUD/FLS check is not needed. How do I make the violation go away?

If you determine that the CRUD operation in question is protected by a sanitizer that SFGE doesn’t recognize, you can add an [engine directive](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to let SFGE know that the CRUD operation _is_ in fact safe.

#### Q: I didn’t get any violations. Does this mean my code is secure?

If you didn’t get any violations, one these is a possibility:

1. SFGE did not identify any entry points
2. SFGE ran into errors for all the entry points identified
3. Your code is actually secure

Since #1 and #2 exist, you may still want to manually make sure your code is secure.

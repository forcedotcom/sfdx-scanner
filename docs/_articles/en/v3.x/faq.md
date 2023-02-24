---
title: 'Frequently Asked Questions'
lang: en
redirect_from: /en/faq
---
## Salesforce Code Analyzer FAQ

### General Questions

#### Q: What is Salesforce Code Analyzer (Code Analyzer)?
A: Code Analyzer is a [Salesforce CLI](https://developer.salesforce.com/docs/atlas.en-us.sfdx_cli_plugins.meta/sfdx_cli_plugins/cli_plugins_architecture.htm) plug-in that helps developers write better and more secure code.

To inspect your code, Code Analyzer uses multiple code analysis engines, including PMD, ESLint, RetireJS, and Salesforce Graph Engine. It identifies potential problems, from inconsistent naming to security vulnerabilities, including advanced vulnerabilities such as lack of Create Read Update Delete/Field-Level Security (CRUD/FLS) checks. Code Analyzer conveys these problems with easy-to-understand results. Run the code analyzer on-command in the CLI, or integrate it into your Continuous Integration/Continuous Development (CI/CD) framework so that you can run it against every code change or on a scheduled basis.

#### Q: Is Code Analyzer part of the AppExchange security review process?
A: Salesforce Code Analyzer is separate from the AppExchange security review process, but it enforces many of the same rules. It can be executed at-will, it provides results in minutes, and it helps you to find and fix problems quickly. As a result, you can be more confident in the code that you submit for security review.

#### Q: Is Code Analyzer only for Salesforce CLI (sfdx) projects?
A: Code Analyzer is compatible with any codebase.

#### Q: Are there prerequisites to use Code Analyzer?
A: You must:

* Install Salesforce CLI on your computer.
* Use Java v1.8 or later.

#### Q: How do I provide feedback on Code Analyzer?
A: Use [this form](https://www.research.net/r/SalesforceCA) to give us your feedback. And thanks!

#### Q: How do I update Code Analyzer?
A: You must:

* Update the plug-in to the latest version by following [these instructions](./en/v3.x/getting-started/install/#upgrade-plug-in).
* To update to a specific version of the plug-in, run: `sfdx plugins:install @salesforce/sfdx-scanner@{{ site.data.versions-v2.scanner }}`

#### Q: How can I use Code Analyzer in my CI/CD?
A: Use the `sfdx scanner:run` command in any scripts used by your CI/CD. We also recommend that you:

* Keep an artifact of the results. Use the `-o | --outfile` flag to write your results to a file.
* If any violations meet or exceed the provided value, use the `-s | --severity-threshold` flag,. The `-v | --violations-cause-error` flag has been deprecated.

### Questions about Languages

#### Q: What languages does Code Analyzer support?
By default, Code Analyzer supports code written in Apex, VisualForce, Java, JavaScript, XML, and TypeScript. To add support for Lightning Web Components, invoke the `scanner:run` command with `--engine eslint-lwc`.

#### Q: How do I get support for additional languages?
A: Create a request in our [Github repo](https://github.com/forcedotcom/sfdx-scanner).

#### Q: How do I enable engine X's default rules for language Y?
A: That depends on the engine in question. Currently, only PMD has multi-language support. To enable PMD’s default rules for a language, add the language’s name to PMD’s `supportedLanguages` array in `~/.sfdx-scanner/Config.json`.

If the language isn’t already supported, create an issue in our [Github repo](https://github.com/forcedotcom/sfdx-scanner).

#### Q: How do I add new rules for language X?
A: Currently, you can only add custom rules for PMD. 

* Ruleset files, and category files defining only XPath-based rules, are referenced as standalone XML files. 
* Java-based rules must be bundled into a JAR. The JAR/XML can then be added to the rule registry with the `scanner:rule:add` command.

If the language isn’t already supported, create an issue on our [Github repo](https://github.com/forcedotcom/sfdx-scanner).

### Questions about Severity Thresholds and Normalization

#### Q: How do I set a severity threshold for a Code Analyzer run?
A: When you run Code Analyzer with the `-s | --severity-threshold` flag and a threshold value, Code Analyzer throws an error if violations are found with equal or greater severity than the provided value. Values are 1 (high), 2 (moderate), and 3 (low). The exit code equals the severity of the most severe violation detected. For example, if a violation of severity 2 is found and the threshold is 2 or 3, then the exit code is 2. Using this flag also implicitly invokes the `--normalize-severity` flag.

#### Q: How do I normalize severity?
A: PMD, ESLint, and RetireJS all have different scales for reporting the severity of violations. When you run Code Analyzer with the `--normalize-severity` flag, Code Analyzer normalizes the severity of violations across all invoked engines. 
* A normalized severity of 1 (high), 2 (moderate), and 3 (low) is returned in addition to the engine specific severity. Applicable engines include: 
	- CSV
	- HTML
	- JSON
	- JUnit-formatted XML
	- SARIF-formatted JSON
	- Table (default output)
	- XML
* For HTML output format, the normalized severity is displayed instead of the engine severity.

#### Q: How is the severity normalized across all the engines?
A: Severity is normalized across all engines using the values in this table.

| Normalized Severity | PMD     | ESLint | ESLint-LWC | ESLint-TypeScript | Retire-JS | Salesforce Graph Engine|
| ------------------- | ------- | ------ | ---------- | ----------------- | --------- | ---------------------- |
| 1 (High)            | 1       | 2      | 2          | 2                 | 1         | 1        			   |
| 2 (Moderate)        | 2       | 1      | 1          | 1                 | 2         | 2      			       |
| 3 (Low)             | 3, 4, 5 |        |            |                   | 3		  | 3					   |

### Questions about CPD

#### Q: What languages are supported by CPD in Code Analyzer?
A: Code Analyzer supports Apex, Java, Visualforce, and XML in CPD.

#### Q: How do I know which files were not included by CPD execution?
A: Execute Code Analyzer for CPD with `--verbose` option. Files are filtered first by `targetPatterns` provided in `~/.sfdx-scanner/Config-pilot.json` file. Files that aren't handled by CPD are listed in this message: `Path extensions for the following files will not be processed by CPD`. 

#### Q: I have a file pattern for a supported languages that isn't picked up by CPD. How do I add the file pattern?
A: Add your file pattern to the CPD engine’s `targetPatterns` in `~/.sfdx-scanner/Config.json`. If you rerun Code Analyzer with the CPD engine option, and the file is still excluded, create an issue on our GitHub repo.

#### Q: In my violation messages from the CPD engine, I’m seeing multiple groups of the same checksum. The code fragment is also identical. Why aren’t these made up of the same group?
A: This is a [known issue](https://github.com/pmd/pmd/issues/2438) in CPD, , and it’s on our backlog to address.

## Questions about Salesforce Graph Engine

#### Q: Why does analyzing my code using Graph Engine take much longer than all the other engines put together?

Graph Engine builds up the context of the source code in its entirety before it applies rules to capture violations. The number of conditionals, classes to instantiate, and method invocations add to the code complexity. Depending on the code complexity, some projects can take longer to process than others. As a result, we recommend that you focus Graph Engine scans on specific entry points after you do an initial full scan. Also consider running full scans on a scheduled basis, such as nightly, while doing more targeted scans when code changes.

#### Q: My code isn’t guarded with an Apex script Create Read Update Delete/Field-Level Security (CRUD/FLS) check. Is there a way to suppress violations on it?

To learn how, read about [Engine Directives](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives).

### Questions about Interpreting ApexFlsViolationRule results

#### Q: My data operation is already protected though not through a CRUD/FLS check. I'm confident that a CRUD/FLS check is not needed. How do I make the violation go away?

If you determine that the CRUD operation in question is protected by a sanitizer that Graph Engine doesn’t recognize, you can add an [engine directive](./en/v3.x/salesforce-graph-engine/working-with-sfge/#add-engine-directives) to let Graph Engine know that the CRUD operation _is_ in fact safe.

#### Q: I didn’t get any violations. Does this mean my code is secure?

If you didn’t get any violations, one these is a possibility:

1. Graph Engine did not identify any entry points
2. Graph Engine ran into errors for all the entry points identified
3. Your code is actually secure

Since #1 and #2 exist, you may still want to manually make sure your code is secure.

### Questions About `OutOfMemory` Errors

#### What Factors Contribute to an `OutOfMemory` Error?

Several factors can degrade Graph Engine’s efficiency and increase the probability of encountering an `OutOfMemory` error.

* With every conditional or method invocation in your code, the number of paths Graph Engine creates increases exponentially.
* Your OS type, Java setup, and other processes running on your machine can influence the heap space assigned by Java Virtual Machine (JVM).

If Graph Engine’s execution is interrupted, it returns results from the portion of source code that it analyzed. We recommend that you add the `--outfile` parameter to capture your results in your own separate file.

#### How Does Graph Engine Determine That a Path Is Too Complex?
When Graph Engine traverses a path, it creates instances of `ApexPathExpander`. The more complex the path, the more instances of `ApexPathExpander` it creates.

Based on the parameters that you provide in your execution, Graph Engine determines a path expansion limit to limit the number of `ApexPathExpander` instances that are created. When this limit is reached while analyzing a path, Graph Engine preemptively aborts the analysis on that path with a `LimitReached` violation. Graph Engine moves on to analyze the next path.

#### How Can You Control This Limit?
Modify the path expansion limit using either of these methods.

* Increase the heap space using `--sfgejvmargs "-Xmx<size>"`
* Modify path expansion limit using `--pathexplimit <new_limit_number>`

Note: If `--pathexplimit` is set to -1, there’s no upper limit check made on the registry.

#### How is the Path Expansion Limit Calculated?
By default, four threads execute within Graph Engine. Based on our analysis, we predict that an `OutOfMemory` occurs when one of the threads approaches 50% of the allotted heap space. The majority of this 50% is occupied by instances of `ApexPathExpander` in the registry.

Using this information, the formula to calculate the limit placed by default on `ApexPathExpander` registry is:

	` `ApexPathExpander` registry limit = 50% of Max Heap Space / Average size of `ApexPathExpander` instance`

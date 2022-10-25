---
title: 'RetireJS'
lang: en
---
## What is RetireJS?
[RetireJS](https://retirejs.github.io/retire.js/) is an engine that analyzes a project’s third-party JavaScript dependencies and identifies security vulnerabilities. It has a thriving community, and its database of vulnerable libraries is updated frequently.

## How does Salesforce Code Analyzer use RetireJS?
Salesforce Code Analyzer (Code Analyzer) uses RetireJS to scan for vulnerable third-party libraries that are bundled into a project.

Files representing vulnerable dependencies are detected in three ways:
* By their name
* By examining their content
* By examining the contents of ZIP

For example, this command scans MyProject for vulnerable third-party libraries.

```bash
$ sfdx scanner:run --engine retire-js --target '/path/to/MyProject' --format csv
```
If `MyProject` contains `MyProject/lorem/ipsum/jquery-3.1.0.min.js`, it’s identified as a vulnerability.

If you rename the file to ```SomeGenericFile.js``` or ```jquery.resource```, or if you hide it within a ZIP file such as ```AllMyLibs.zip```, Code Analyzer still identifies the vulnerable library and reports it as a violation.

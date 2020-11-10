---
title: 'RetireJS'
lang: en
---
## What is RetireJS?
[RetireJS](https://retirejs.github.io/retire.js/) is an engine that analyzes a project's third-party JavaScript
dependencies and identifies those that have known security vulnerabilities. It has a lively and responsive community,
and its database of vulnerable libraries is updated frequently.

## How does Salesforce CLI Scanner use RetireJS?
Salesforce CLI Scanner uses RetireJS to scan for vulnerable third-party libraries that are bundled into a project.

Files representing vulnerable dependencies are detected by their name *or* by examining their content, and the scanner
can even examine the contents of a ZIP to find vulnerabilities within.

For example, consider the following command, which will scan `MyProject` for vulnerable third-party libraries:
```bash
$ sfdx scanner:run --engine retire-js --target '/path/to/MyProject' --format csv
```
If `MyProject` contains `MyProject/lorem/ipsum/jquery-3.1.0.min.js`, this will be identified as a vulnerability.

If the file were renamed to `SomeGenericFile.js` or `jquery.resource`, or even hidden within a ZIP such as `AllMyLibs.zip`,
the scanner will still identify the vulnerable library and report it as a violation.

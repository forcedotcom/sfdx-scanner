---
title: CI/CD Integration
lang: en
redirect_from: /en/ci-cd-integration
---
## CI/CD Integration
We recommend that you integrate Salesforce Code Analyzer into your development process to scan your code regularly for potential problems. Code Analyzer makes it easier to identify issues ahead of submitting your code for the AppExchange Security Review.

To integrate Salesforce Code Analyzer into your Continuous Integration/Continuous Development (CI/CD) tool, call the appropriate run command in any scripts used by your CI/CD. We recommend that you run:

* `sf scanner run` whenever CI/CD detects changes to code.
* `sf scanner run dfa` with `--target=<all classes>` on a scheduled basis, such as nightly.

Why do we recommend that `sf scanner run dfa` is executed only on a scheduled basis? Depending on the number of paths it generates, Salesforce Graph Engine can take some time to execute when you include all target classes. Alternatively, you can limit the number of paths and speed up Graph Engine execution in your CI/CD by reducing the number of targets using the flag `--target=<specific class>` as you can see [here](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/scanner-commands/dfa/).

Follow these CI/CD best practices

* Use the `--format=junit` flag, a standard format used by the CI/CD tool.
* Use the `-o/--outfile=<name>.xml` flag to write your results to a file and produce a results artifact that can be used by the CI/CD tool.
* Use the `-s/–severity-threshold` flag to cause a non-zero exit code when any violations meet or exceed the provided value. Many CI/CD tools require thresholds.

## Accelerate Your CI/CD Integration

Our community of users continues to develop templates and tools that help you speed up Code Analyzer integration into your CI/CD process.

Try these community tools:

* [SFDX Scan Pull Request](https://github.com/marketplace/actions/sfdx-scan-pull-request) by Mitchell Spano
* [Pull Request SFDX Code Review](https://github.com/marketplace/actions/pull-request-sfdx-code-review) by Aleš Remta

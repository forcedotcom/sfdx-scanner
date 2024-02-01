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

## Accelerate Your CI/CD Integration #

### Run Code Analyzer GitHub Action

To accelerate your continuous integration/continuous (CI/CD) development, create a GitHub Action workflow that uses the `run-code-analyzer` GitHub Action. GitHub Action workflows provide opportunities to automate your entire pipeline, from building and testing to deployment. The `run-code-analyzer` GitHub Action scans your code for violations using Salesforce Code Analyzer, uploads the results as an artifact, and displays the results as a job summary. 

If you’re using DevOps Center, you can use the `run-code-analyzer` GitHub Action as you promote changes, helping you identify and address issues earlier in your development pipeline.

With the `run-code-analyzer` Action, you can customize:

* Which Salesforce Code Analyzer command to run: run or run dfa
* What arguments to pass with your scan
* The name of the results artifact

With `run-code-analyzer`, these outputs are included in the results file.

* The Salesforce Code Analyzer execution exit code
* The total number of violations found
* The number of normalized low-,  medium-, and high-severity violations found

For usage info on the `run-code-analyzer` GitHub Action, read [run-code-analyzer documentation](https://github.com/marketplace/actions/run-salesforce-code-analyzer) on the [GitHub Actions Marketplace](https://github.com/marketplace).

## Other CI/CD Tools

Our community of users continues to develop templates and tools that help you speed up Code Analyzer integration into your CI/CD process.

Try these community tools:

* [SFDX Scan Pull Request](https://github.com/marketplace/actions/sfdx-scan-pull-request) by Mitchell Spano
* [Pull Request SFDX Code Review](https://github.com/marketplace/actions/pull-request-sfdx-code-review) by Aleš Remta

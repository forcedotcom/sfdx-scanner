---
title: 'RetireJS'
lang: en
---
## What is RetireJS?
[RetireJS](https://retirejs.github.io/retire.js/) is an engine that analyzes a project's third-party JavaScript
dependencies and identifies those that have known security vulnerabilities. It has a lively and responsive community,
and its database of vulnerable libraries is updated frequently. Starting v3.0.0, RetireJS is included in the default set of engines.

## How does Salesforce Code Analyzer use RetireJS?
Salesforce Code Analyzer uses RetireJS to scan for vulnerable third-party libraries that are bundled into a project.

Files representing vulnerable dependencies are detected by their name *or* by examining their content, and the code analyzer
can even examine the contents of a ZIP to find vulnerabilities within.

For example, consider the following command, which will scan `MyProject` for vulnerable third-party libraries:
```bash
$ sfdx scanner:run --engine retire-js --target '/path/to/MyProject' --format csv
```
If `MyProject` contains `MyProject/lorem/ipsum/jquery-3.1.0.min.js`, this will be identified as a vulnerability.

If the file were renamed to `SomeGenericFile.js` or `jquery.resource`, or even hidden within a ZIP such as `AllMyLibs.zip`,
the code analyzer will still identify the vulnerable library and report it as a violation.

By default, we return a truncated version of RetireJS's internal violations, indicating that the library in question is out of date. To receive more thorough information, you may use the `--verbose-violations` flag. When **not** using `--verbose-violations`, a violation message looks like: 
```
jquery v3.1.0 is insecure. Please upgrade to latest version.
```
When using `--verbose-violations`, a violation message looks like:
```
jquery 3.1.0 has known vulnerabilities:
severity: medium; summary: jQuery before 3.4.0, as used in Drupal, Backdrop CMS, and other products, mishandles jQuery.extend(true, {}, ...) because of Object.prototype pollution; CVE: CVE-2019-11358; https://blog.jquery.com/2019/04/10/jquery-3-4-0-released/ https://nvd.nist.gov/vuln/detail/CVE-2019-11358 https://github.com/jquery/jquery/commit/753d591aea698e57d6db58c9f722cd0808619b1b
severity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11022; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/
severity: medium; summary: Regex in its jQuery.htmlPrefilter sometimes may introduce XSS; CVE: CVE-2020-11023; https://blog.jquery.com/2020/04/10/jquery-3-5-0-released/
```
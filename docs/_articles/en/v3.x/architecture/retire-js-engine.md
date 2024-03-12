---
title: 'RetireJS'
lang: en
redirect_from: /en/architecture/retire-js-engine
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/retirejs-engine.html
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
$ sf scanner run --engine retire-js --target '/path/to/MyProject' --format csv
```
If `MyProject` contains `MyProject/lorem/ipsum/jquery-3.1.0.min.js`, it’s identified as a vulnerability.

If you rename the file to ```SomeGenericFile.js``` or ```jquery.resource```, or if you hide it within a ZIP file such as ```AllMyLibs.zip```, Code Analyzer still identifies the vulnerable library and reports it as a violation.

By default, we return a truncated version of RetireJS's internal violations which indicates that the library in question is out of date. To receive more information, use the `--verbose-violations` flag. 

A `--verbose-violations` violation message looks like this: 
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

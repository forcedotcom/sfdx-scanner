---
title: 'CPD'
lang: en
redirect_from: /en/architecture/cpd-engine
redirect_to: https://developer.salesforce.com/docs/platform/salesforce-code-analyzer/guide/cpd-engine.html
---
## What is Copy/Paste Detector (CPD)?
Shipped with PMD, CPD helps identify blocks of duplication across files. 

## How can you use CPD through Salesforce Code Analyzer?

By default, the CPD engine isn’t enabled and isn’t run with a generic ```scanner run``` command. To specifically invoke CPD, you use the ```--engine``` option, like this:

`sf scanner run --target "/some/path" --engine cpd`

## Understanding the violation message
Because CPD returns duplicated code fragments, meaningful output contains more than one file as a part of a group. Salesforce Code Analyzer (Code Analyzer) represents each group by a short checksum of the corresponding code fragment. Every violation message contains this checksum, the number of tokens in the checksum, the total number of occurrences of this duplicated code, and the index of the current occurrence. This information can help you understand the impact of the duplication.

Consider this sample violation thrown from the CPD engine.

```
{
    "line":"1",
    "column":"1",
    "endLine":"39",
    "endColumn":"12",
    "ruleName":"copy-paste-detected",
    "severity":3,
    "message":"c42bf68: 1 of 5 duplication segments detected. 39 line(s), 242 tokens.",
    "category":"Copy/Paste Detected",
    "url":"https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates"
}
```

The message shows that there are four other duplicated segments in other files. Find these segments by searching for ```c42bf68``` among the remaining violations detected. The number of lines and tokens in the message shows how large the duplicated block is. The `line`, `column`, `endLine`, and `endColumn` values help identify the actual block of code fragment within the file.

## See Also
- [CPD](https://pmd.github.io/latest/pmd_userdocs_cpd.html)
- [PMD Source Code Analyzer Project: Why should you care about duplicates?](https://pmd.github.io/latest/pmd_userdocs_cpd.html#why-should-you-care-about-duplicates)

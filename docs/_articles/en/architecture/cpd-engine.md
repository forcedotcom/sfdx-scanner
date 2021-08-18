---
title: 'CPD'
lang: en
---
## What is CPD?
[CPD](https://pmd.github.io/latest/pmd_userdocs_cpd.html) is a copy/paste detector shipped with PMD. It helps identify blocks of duplication across files. [Here](https://pmd.github.io/latest/pmd_userdocs_cpd.html#why-should-you-care-about-duplicates) are some reasons to avoid code duplication in general.

## How can you use CPD through Salesforce CLI Scanner?

By default, CPD engine is not enabled and is not run with a generic `scanner:run` command. To specifically invoke CPD, you can use the `--engine` option like this:

`sfdx scanner:run --target "/some/path" --engine cpd`

## Understanding the violation message
Since CPD returns duplicated code fragments, a meaningful output contains more than one file as a part of the grouping. CLI Scanner represents each group by a short checksum of the corresponding code fragment. Every violation message contains this checksum, the number of tokens in the checksum, the total number of occurrences of this duplicated code, and the index of the current occurrence. This information can help understand the impact of the duplication.

For example, consider this sample violation thrown from CPD engine:
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

The message shows that there are 4 other duplicated segments found in other files. You can find these by searching for `c42bf68` amongst the remaining violations detected.
The number of lines and tokens in the message shows how large the duplicated block is.
The `line`, `column`, `endLine`, and `endColumn` values help identify the actual block of code fragment within the file.


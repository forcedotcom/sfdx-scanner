# Writing Graph Engine-Friendly Code

Graph Engine is a powerful tool for code analysis, but it has its own set of limitations. Read this guide to learn about Graph Engine’s nuanced architecture, and learn how to make small refactors to your code to significantly improve Graph Engine's performance.

**Note**: Some suggested refactors in this guide can conflict with accepted best practices for Apex. Read through the options, and update your code as appropriate.

## Who Should Read This Guide?
If you have a highly complex codebase, and you tried the existing mechanisms for [avoiding timeouts](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/troubleshooting/#issues-using-salesforce-graph-engine) and [path expansion limits](https://forcedotcom.github.io/sfdx-scanner/en/v3.x/salesforce-graph-engine/working-with-sfge/#understand-outofmemory-errors), this guide is for you. Use the suggestions in this guide to refactor your code, be more compatible with Graph Engine's architecture, and find performance gains.

## Understanding Salesforce Graph Engine

### How Graph Engine Spends Its Time
At a high level, Graph Engine identifies entry points into your codebase. Next, Graph Engine analyzes these entry points by using them to build paths through the code and traverses those paths to apply rules.

Two factors directly affect Graph Engine’s performance.

* The number of entry points analyzed. Entry points are evaluated in parallel, and the timeout is applied to each entry point separately.
* The number of paths each entry point constructs. Building paths is the most expensive part of analyzing an entry point. Typically, an entry point causes timeouts or memory issues because it constructs an inordinate number of paths.

Based on these factors, the most consequential refactoring that you can do is to decrease the number of paths a single entry point constructs, or to more evenly distribute paths between entry points.

### How Paths Are Built
To understand how to decrease the number of paths in a given entry point, let’s review this example to see how paths are built.

**Example**:

```public void foo(boolean b1, boolean b2) {
    if (b1) {
        System.debug('b1 is true');
    } else {
        System.debug('b1 is false');
    }
    if (b2) {
        System.debug('b2 is true');
    } else {
        System.debug('b2 is false');
    }
}
```

Assume the value of parameter `b1` and `b2` are unknown. Graph Engine identifies four unique paths through the method. Why? When the values of these two parameters are unknown, both `if` expressions have an indeterminate outcome. One path must fork into two, and then into four.

Now assume that the value of parameter `b1` is known. Its corresponding `if` expression can be determined in advance, so Graph Engine doesn’t fork at that expression. The number of paths drops from four to two.

In other words, where possible, Graph Engine avoids building impossible paths.

## Refactoring Entry Points

If an entry point’s complexity causes it to encounter timeouts or memory problems, the easiest way to resolve these errors is to refactor that entry point into two separate entry points with fewer paths. These entry points are analyzed in parallel and have separate timeouts. These entry points also complete faster and are less likely to exceed time or memory limits.

Refactoring entry points to redistribute paths is low effort and low risk, but it is also powerful in its ability to make your code more compatible with Graph Engine.


### Code Example: One Inefficient Entry Point, One Path
Let’s look at an example with an Aura-Enabled method. Graph Engine considers this method an entry point.

**Example**:

```
@AuraEnabled
public void foo(boolean b1, boolean b2, boolean b3) {
    if (b1) {
        helperMethod1a();
    } else {
        helperMethod1b();
    }
    if (b2) {
        helperMethod2a();
    } else {
        helperMethod2b();
    }
    if (b3) {
        helperMethod3a();
    } else {
        helperMethod3b();
    }
}
```

The values of all three parameters, `b1`, `b2`, and `b3`, are unknown, so each conditional `if` expression causes paths to fork. If we assume that each helper method contains exactly one unique path, then the total number of possible paths is 8, or 1 x 2 x 2 x 2. That number of paths is unlikely to exceed time or memory limits. 

### Code Example: One Inefficient Entry Point, Ten Paths

Now let’s use the same example but assume that each helper method contains 10 unique paths. Then each conditional `if` expression actually creates 20 unique paths instead of just two. The total number of paths is then 8,000, or 1 x 20 x 20 x 20, which is an excessive number of paths.

### Code Example with Two Efficient Entry Points, Fewer Paths

Let’s refactor the code from our previous example. We turn the single entry point into two entry points that include fewer paths. Our code now looks something like this.

**Example**:

```
@AuraEnabled
public void foo1(boolean b1, boolean b2) {
    innerFoo(b1, b2, true);
}

@AuraEnabled
public void foo2(boolean b1, boolean b2) {
    innerFoo(b1, b2, false);
}

public void innerFoo(boolean b1, boolean b2, boolean b3) {
    if (b1) {
        helperMethod1a();
    } else {
        helperMethod1b();
    }
    if (b2) {
        helperMethod2a();
    } else {
        helperMethod2b();
    }
    if (b3) {
        helperMethod3a();
    } else {
        helperMethod3b();
    }
}
```

Our refactored code with redistributed paths has two Aura-enabled entry points, `foo1` and `foo2`. Each of these entry points invokes `innerFoo` by passing in their own parameters and a literal boolean to `innerFoo`, which is identical to the old `foo`.

Each entry point uses a definitive value for `b3` in its invocation of `innerFoo`, so each entry point avoids forking at the final `if`. That means half as many paths are evaluated. If we assume that each helper method has 10 unique paths, each entry point builds only 4,000 paths, or 1 x 20 x 20 x 10. 

| Entry Points | Unknown Parameters | Indeterminate if Clauses | Paths Per Helper Method | Paths Per Entry Point Calculation | Total Paths Per Entry Point |
| -------- | ----------- | ---------- |----------- | ----------- | ----------- |
| One Inefficient, One Path | 3 |3 | 1 | 1 x 2 x 2 x 2 | 8 |
| One Inefficient, Ten Paths | 3 |3 | 10 | 1 x 20 x 20 x 20 | 8,000 |
| Two Sfficient | 2 |2 | 10 | 1 x 20 x 20 x 10 | 4,000 |

By refactoring the code from the Inefficient example into two efficient entry points, we cut the total paths per entry point from 8,000 to 4,000. The new entry points are far less likely to time out than the original, and because they can be analyzed in parallel, they finish sooner.

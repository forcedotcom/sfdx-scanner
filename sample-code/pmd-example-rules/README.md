# PMD example rules
Salesforce Code Analyzer supports the use of custom PMD rules. If those rules involve any Java code,
then that Java must be compiled into a JAR before it can be registered with Code Analyzer.

This project is an example of how to write Java-based PMD rules, and use Maven to compile them into a JAR.

## How do I build this project?
1. Make sure [Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html) is set up on your machine.
2. Clone the Code Analyzer project.
3. From the `sample-code/pmd-example-rules` directory, run `mvn clean package`.
4. Observe that a new folder was created named `target`, containing (among other things) a file named `pmd-example-rules-1.0-SNAPSHOT.jar`.
5. From the `sample-code/pmd-example-rules` directory, run `sfdx scanner:rule:add --language apex --path ./target/pmd-example-rules-1.0-SNAPSHOT.jar`.
6. Run `scanner:rule:list --category CustomRules --engine pmd` to confirm the addition of the new rule.
7. Run `scanner:run --category CustomRules --engine pmd --target path/to/any/apexfile.cls` and observe a violation for the new rule.

## What else can I do with this project?
You can use this project as a template for how to define and build your own custom rules, categories, and rulesets.

# PMD Example Rules
Salesforce Code Analyzer supports the use of custom PMD rules. If your custom PMD rules include Java code,
then you must compile your Java code into a JAR before you register it with Code Analyzer.

This project is an example of how to write Java-based PMD rules, and use Maven to compile them into a JAR.

## How do I build this project?
1. Install [Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html).
2. Clone the [Code Analyzer](https://github.com/forcedotcom/sfdx-scanner) project.
3. From the `sample-code/pmd-example-rules` directory, run `mvn clean package`.
4. Notice the new folder was created named `target`. It contains a file named `pmd-example-rules-1.0-SNAPSHOT.jar`.
5. From the `sample-code/pmd-example-rules` directory, run `sfdx scanner:rule:add --language apex --path ./target/pmd-example-rules-1.0-SNAPSHOT.jar`.
6. Run `scanner:rule:list --category CustomRules --engine pmd` to confirm the addition of the new rule.
7. Run `scanner:run --category CustomRules --engine pmd --target path/to/any/apexfile.cls` and view the new rule violation.

## What else can I do with this project?
Use this project as a template for how to define and build your own custom rules, categories, and rulesets.

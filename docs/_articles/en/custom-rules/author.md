---
title: Authoring Custom Rules
lang: en
---

## What are Custom Rules?

Let's say your codebase has specific and repetitive coding issues that you want to address and clean up. Ideally you'd use the built-in rules of the Salesforce CLI Scanner to find these rule violations. But sometimes the problems exist only in the context of your codebase, and the built-in rules may not catch them. In this case, create your own _custom rules_ to highlight these issues as rule violations when you scan your code.

PMD and Eslint's custom rules work very differently. This causes Scanner plugin to deal with both types in distinctly different ways. Please note that information related to PMD Custom Rules does NOT apply to Eslint Custom Rules.

---

## PMD Custom Rules

### Writing PMD Custom Rules

Here are the [instructions](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html) on how to write PMD Custom Rules. PMD Rules may be either XPath-based or Java-based, and these rule types must be authored differently.
<br>
To be compatible with the Salesforce CLI Scanner, PMD custom rules must also meet the following criteria:

-   Rules must be **defined** in XML files whose path matches the format `<some base dir>/category/<language>/<filename>.xml`.
-   XPath-based rules can be contained in standalone XML files.
-   Java-based rules must be compiled, and bundled into a JAR.
-   Custom rulesets consisting of references to existing rules may be contained in standalone XML files whose path matches the format `<some base dir>/rulesets/<language>/<filename>.xml`

### Compiling Java-Based PMD Custom Rules

Compiling custom rules using Java can be accomplished using [Maven](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html).

NOTE: The below instructions are written for Unix based operating systems.

#### Directory Structure

Create a directory to hold your custom rules with a structure that matches the tree below, substituting your organization's name for `organization`:

```
.
├── pom.xml
└── src
    └── main
        ├── java
        │   └── com
        │       └── organization
        │           └── pmd
        │               └── MyRule.java
        └── resources
            └── category
                └── apex
                    └── customRules.xml
```

##### `MyRule.java`

Here is a sample rule for demonstration. To learn more about which classes are available for custom rule compilation, please view the [PMD documentation](https://javadoc.io/static/net.sourceforge.pmd/pmd-apex/{{ site.data.versions.pmd }}/index.html).

```java
package com.organization.pmd;

import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

public class MyRule extends AbstractApexRule {

  @Override
  public Object visit(ASTUserClass theClass, Object data) {
    asCtx(data).addViolation(theClass);
    return data;
  }

}
```

##### `customRules.xml`

Create a custom XML rule [definition file](https://pmd.github.io/latest/pmd_userdocs_extending_writing_rules_intro.html#xml-rule-definition) which references the rule.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    name="CustomRules"
    xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>Custom Rules</description>
    <rule
        name="MyRule"
        language="apex"
        class="com.organization.pmd.MyRule"
        message="This is a demonstration rule"
        externalInfoUrl="http://foo.com/bar/MyRule"
    >
        <description>Demonstration</description>
        <priority>1</priority>
    </rule>
</ruleset>
```

#### Compilation using Maven

The `pom.xml` file is an integral part of a Maven project. It tells Maven which dependencies to fetch from the Maven repository and how to build the artifacts for the project. Below is a `pom.xml` file which you can use to compile Java-based custom rules.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.organization.pmd</groupId>
  <artifactId>custom-pmd-rules</artifactId>
  <version>1.0.0</version>

  <name>custom-pmd-rules</name>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
  </properties>

  <dependencies>
    <dependency>
      <groupId>net.sourceforge.pmd</groupId>
      <artifactId>pmd</artifactId>
      <version>{{ site.data.versions.pmd }}</version>
      <type>pom</type>
    </dependency>
    <dependency>
      <groupId>net.sourceforge.pmd</groupId>
      <artifactId>pmd-apex</artifactId>
      <version>{{ site.data.versions.pmd }}</version>
    </dependency>
    <dependency>
      <groupId>net.sourceforge.pmd</groupId>
      <artifactId>pmd-apex-jorje</artifactId>
      <version>{{ site.data.versions.pmd }}</version>
      <type>pom</type>
    </dependency>
    <dependency>
      <groupId>org.ow2.asm</groupId>
      <artifactId>asm</artifactId>
      <version>9.4</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.1</version>
        <configuration>
          <release>11</release>
        </configuration>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <includes>
            <include>com/**/*.class</include>
            <include>category/apex/*.xml</include>
          </includes>
          <archive>
            <manifest>
              <addClasspath>true</addClasspath>
            </manifest>
            <addMavenDescriptor>false</addMavenDescriptor>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

With the `pom.xml` defined, we can now compile our rules:

```shell
$ mvn package
```

This will produce a directory called `target` which contains a file called `custom-pmd-rules-1.0.0.jar`. This `.jar` file contains the compiled version of our rule along with the XML rule definition:

```shell
$ unzip -l target/custom-pmd-rules-1.0.0.jar

# produces the following output:

Archive:  target/custom-pmd-rules-1.0.0.jar
  Length      Date    Time    Name
---------  ---------- -----   ----
        0  01-01-2023 00:00   META-INF/
      697  01-01-2023 00:00   META-INF/MANIFEST.MF
        0  01-01-2023 00:00   category/
        0  01-01-2023 00:00   category/apex/
      641  01-01-2023 00:00   category/apex/customRules.xml
        0  01-01-2023 00:00   com/
        0  01-01-2023 00:00   com/organization/
        0  01-01-2023 00:00   com/organization/pmd/
      775  01-01-2023 00:00   com/organization/pmd/MyRule.class
---------                     -------
     2113                     9 files
```

### Adding Compiled Java-based Rules

Now that our custom rule is defined, referenced in an XML rule definition file, and compiled into a `.jar`, we can add the rule to the scanner:

```shell
$ sfdx scanner:rule:add \
  --language apex \
  --path /path/to/rules/directory/target/custom-pmd-rules-1.0.0.jar
```

#### Executing Java-based Rules

To validate that our custom rules are executing properly, execute the sfdx-scanner plugin on a sample class and reference the XML rule definition file using the `--pmdconfig` flag:

```shell
$ sfdx scanner:run \
  --target /path/to/salesforce/directory/force-app/main/default/classes/MyClass.cls \
  --pmdconfig /path/to/rules/directory/src/main/resources/category/apex/customRules.xml

```

##### XML Rule Definition Composition

Referencing a specific rule's XML definition file is less than ideal as it limits the scope of the findings to only the specific rules we have defined.

Because we included the `category/apex/customRules.xml` in the compiled `.jar`, we can compose our our rule definitions into one `all-rules.xml` file which references other XML rule definition file(s):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ruleset xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    name="AllRules"
    xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
    <description>All Rules</description>
    <!-- Standard Rules -->
    <rule ref="category/apex/bestpractices.xml" />
    <rule ref="category/apex/codestyle.xml" />
    <rule ref="category/apex/design.xml" />
    <rule ref="category/apex/documentation.xml" />
    <rule ref="category/apex/errorprone.xml" />
    <rule ref="category/apex/multithreading.xml" />
    <rule ref="category/apex/performance.xml" />
    <rule ref="category/apex/security.xml" />
    <!-- Custom Rules -->
    <rule ref="category/apex/customRules.xml" />
</ruleset>
```

```shell
$ sfdx scanner:run \
  --target /path/to/salesforce/directory/force-app/main/default/classes/MyClass.cls \
  --pmdconfig /path/to/salesforce/directory/all-rules.xml
```

---

## Eslint Custom Rules

### Writing Eslint Custom Rules

Writing custom Eslint rules requires creating a custom Eslint plugin and defining rules within it. Here is [Eslint's official documentation on writing rules](https://eslint.org/docs/developer-guide/working-with-rules). Also, there are many tutorials and blogs that explain this process in detail. In this documentation, we'll focus on the specific elements that help the rule work with the Scanner plugin.

### Adding Rule as NPM Dependency

While writing the rule, please make sure the rule definition contains documentation. We are specifically looking for a format like this:

```bash
// Rule definition in index.js or where you choose to store it
...
    meta: {
        docs: {
            description: "Information about the rule"
        },
		...
	}
...
```

Once the rule is ready and tested, add it as a dependency to the NPM setup in the directory where you to plan to run the Scanner plugin from. You can use `npm` or `yarn` version of this command:

```bash
yarn add file:/path/to/eslint-plugin-my-custom
```

Once added, make sure the `node_modules` directory has child directory with your plugin's name and this directory contains the `package.json`, `index.js` and other files you had created.

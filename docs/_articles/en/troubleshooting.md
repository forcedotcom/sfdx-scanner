---
title: Troubleshooting Common Issues
lang: en
---

Here are some troubleshooting tips to fix common issues when you use the Salesforce CLI Scanner plug-in.

### My custom rule Java file doesn’t compile.
* Make sure that you reference only PMD features and classes that are available in version 6.22.0.
* Check that you're using correct syntax. 
* Check that the compilation CLASSPATH contains the correct version of the PMD binary.

### I successfully created the JAR file and added it using the `scanner:rule:add` command, but `scanner:rule:list` doesn't display my custom rules.

* Check that the XML Rule Definition file is included in the JAR. Run `jar tf /your/jar/file.JAR` to list the files in your JAR. 
* Make sure your XML file is in a PATH that includes `category` as a directory. 
* Check that your class files are included in the JAR. 
* Confirm that the PATH to the class files reflects the package structure in the Java file.
    
### The `scanner:rule:list` command displays my new custom rules in the catalog, but when I run them I get an error about the Java version.

One possible reason is that the Java version you used to build your code is different from the version the Salesforce CLI Scanner plug-in uses to invoke PMD. Make sure you compile your Java code with the same Java version and path that’s listed in the `java-home` key in `<HOME_DIR>/.sfdx-scanner/Config.json`.

### The `scanner:rule:list` command displays my new custom rules in the catalog, but when I run a rule, I get a `ClassNotFoundException`.

One possible reason is that you referenced a class in your custom rule Java code from the PMD library that's not available in version 6.22.0. Make sure that you reference only PMD features and classes that are available in version 6.22.0.



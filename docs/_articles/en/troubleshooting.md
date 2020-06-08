---
title: Troubleshooting Common Issues
lang: en
---

Here’s some information on how to get past roadblocks that you might encounter while using Salesforce Scanner CLI Plugin

### My Custom Rule Java file doesn’t compile
Make sure you DO NOT use PMD features/classes that are not available in version 6.22.0. Please check for basic syntax, etc. Validate that the compilation class path contain the correct version of PMD binary.

### I successfully created the JAR file and added it using the ```scanner:rule:add``` command, but ```scanner:rule:list``` does not display my custom rules.

Validate that the XML Rule Definition file is included in the JAR. You can run ```jar tf /your/jar/file.JAR```. 

Make sure your XML file is in a path that include ```category``` as a directory. Check that your class files included. Confirm that the path to the class files reflect the package structure from the Java file
    
### ```scanner:rule:list``` command displays my new Custom Rules in the catalog, but when I run them, I get an error related to Java version.

Highly possible that the java version used for building your code is different from the version in which PMD is invoked from Sfdx Scanner. 

Make sure you compiled your Java code with the same Java version and path that’s noted in ```java-home``` key in ```<HOME_DIR>/.sfdx-scanner/Config.json```

### ```scanner:rule:list``` command displays my new Custom Rules in the catalog, but when I run them, I get a ClassNotFoundException.

Highly possible that you referenced a class in your Custom Rule Java code from PMD library which is not available in version 6.22.0. Make sure that you reference only PMD features/classes that are available in 6.22.0

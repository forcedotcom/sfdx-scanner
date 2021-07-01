#!/bin/bash
# Auto-generated on Thu Jul 01 2021
#
# This script smoke-tests the entire plugin by running a series of commands that collectively capture a
# vertical slice of the plugin, hitting every major piece of functionality. If they all succeed, we can reasonably
# conclude that the plugin is approximately stable.
#
# DO NOT EDIT THIS SCRIPT DIRECTLY!
# INSTEAD, MAKE CHANGES IN 	./SmokeTestGenerator.js, 

set -e
echo "List all rules w/out filters"
$1 scanner:rule:list
echo "Filter rules by engine"
$1 scanner:rule:list --engine eslint
echo "Describe a real rule"
$1 scanner:rule:describe -n EmptyCatchBlock
echo "Describe a non-existent rule"
$1 scanner:rule:describe -n NotAnActualRule
echo "Run rules against force-app, which should hit PMD and ESLint engines"
$1 scanner:run --format junit --target test/code-fixtures/projects/app/force-app --outfile test-results/run1.xml
echo "Run rules against a typescript file, which should run ESLint-Typescript"
$1 scanner:run --format junit --target test/code-fixtures/projects/ts/src/simpleYetWrong.ts --tsconfig test/code-fixtures/projects/tsconfig.json --outfile test-results/run2.xml
echo "Run RetireJS against a folder"
$1 scanner:run --format junit --engine retire-js --target test/code-fixtures/projects/dep-test-app/folder-a --outfile test-results/run3.xml
echo "Add a JAR of custom rules"
$1 scanner:rule:add --language apex --path test/test-jars/apex/testjar1.jar
echo "List the rules, including the custom ones"
$1 scanner:rule:list --engine pmd
echo "Describe a custom rule"
$1 scanner:rule:describe -n fakerule1
echo "Run a custom rule"
$1 scanner:run --format junit --category SomeCat1 --target test/code-fixtures/projects/app/force-app --outfile test-results/run4.xml
echo "Remove a custom rule"
$1 scanner:rule:remove --path test/test-jars/apex/testjar1.jar --force
echo "List the rules a final time, to make sure nothing broke"
$1 scanner:rule:list
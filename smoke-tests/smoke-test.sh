#!/bin/bash
# Auto-generated on Fri Jul 16 2021
# This script smoke-tests the entire plugin by running a series of commands that collectively capture a vertical slice
# of the plugin, hitting every major piece of functionality. If they all succeed, then we can reasonably assume that
# the plugin is approximately stable.
# DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/SmokeTestGenerator.js AND RERUN THAT SCRIPT
# FROM THE PROJECT ROOT!
set -e
EXE_NAME=$1
echo "====== STARTING SMOKE TEST ======"
echo "==== List all rules w/out filters ===="
$EXE_NAME scanner:rule:list
echo "==== Filter rules by engine ===="
$EXE_NAME scanner:rule:list --engine eslint
echo "==== Describe a real rule ===="
$EXE_NAME scanner:rule:describe -n EmptyCatchBlock
echo "==== Describe a non-existent rule ===="
$EXE_NAME scanner:rule:describe -n NotAnActualRule
echo "==== Run rules against force-app, which should hit PMD and ESLint engines ===="
$EXE_NAME scanner:run --format junit --target test/code-fixtures/projects/app/force-app --outfile smoke-test-results/run1.xml
echo "==== Run rules against a typescript file, which should run ESLint-Typescript ===="
$EXE_NAME scanner:run --format junit --target test/code-fixtures/projects/ts/src/simpleYetWrong.ts --tsconfig test/code-fixtures/projects/tsconfig.json --outfile smoke-test-results/run2.xml
echo "==== Run RetireJS against a folder ===="
$EXE_NAME scanner:run --format junit --engine retire-js --target test/code-fixtures/projects/dep-test-app/folder-a --outfile smoke-test-results/run3.xml
echo "==== Add a JAR of custom rules ===="
$EXE_NAME scanner:rule:add --language apex --path test/test-jars/apex/testjar1.jar
echo "==== List the rules, including the custom ones ===="
$EXE_NAME scanner:rule:list --engine pmd
echo "==== Describe a custom rule ===="
$EXE_NAME scanner:rule:describe -n fakerule1
echo "==== Run a custom rule ===="
$EXE_NAME scanner:run --format junit --category SomeCat1,Security --target test/code-fixtures/projects/app/force-app --outfile smoke-test-results/run4.xml
echo "==== Remove a custom rule ===="
$EXE_NAME scanner:rule:remove --path test/test-jars/apex/testjar1.jar --force
echo "==== List the rules a final time, to make sure nothing broke ===="
$EXE_NAME scanner:rule:list

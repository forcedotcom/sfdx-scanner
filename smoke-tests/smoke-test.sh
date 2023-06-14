#!/bin/bash
# Auto-generated on Wed Jun 14 2023
# This script smoke-tests the entire plug-in by running a series of commands that collectively capture a vertical slice
# of the plug-in, hitting every major piece of functionality. If they all succeed, then we can reasonably assume that
# the plug-in is approximately stable.
# DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/SmokeTestGenerator.js AND RERUN THAT SCRIPT
# FROM THE PROJECT ROOT!
set -e
EXE_NAME=$1
echo "====== STARTING SMOKE TEST ======"
echo "==== Make results directory ===="
mkdir -p smoke-test-results
echo "==== List all rules w/out filters ===="
$EXE_NAME scanner:rule:list
echo "==== Filter rules by engine ===="
$EXE_NAME scanner:rule:list --engine eslint
echo "==== Describe a real rule ===="
$EXE_NAME scanner:rule:describe -n EmptyCatchBlock
echo "==== Describe a non-existent rule ===="
$EXE_NAME scanner:rule:describe -n NotAnActualRule
echo "==== Run rules against force-app, which should hit PMD and ESLint engines ===="
$EXE_NAME scanner:run --format junit --target test/code-fixtures/projects/app/force-app --outfile smoke-test-results/pmd-eslint.xml
echo "==== Run rules against a typescript file, which should run ESLint-Typescript ===="
$EXE_NAME scanner:run --format junit --target test/code-fixtures/projects/ts/src/simpleYetWrong.ts --tsconfig test/code-fixtures/projects/tsconfig.json --outfile smoke-test-results/eslint-typescript.xml
echo "==== Run RetireJS against a folder ===="
$EXE_NAME scanner:run --format junit --engine retire-js --target test/code-fixtures/projects/dep-test-app/folder-a --outfile smoke-test-results/retire-js.xml
echo "==== Run CPD against a folder ===="
$EXE_NAME scanner:run --format junit --engine cpd --target test/code-fixtures/projects/cpd-test-app/src/classes --outfile smoke-test-results,cpd.xm
echo "==== Run PMD with custom config via --pmdconfig flag ===="
$EXE_NAME scanner:run --format junit --engine pmd --target test/code-fixtures/projects/app/force-app --pmdconfig test/test-xml/apex/smoke-config.xml --outfile smoke-test-results/pmd-customconfig.xml
echo "==== Run Salesforce Graph Engine's non-DFA rules against a folder ===="
$EXE_NAME scanner:run --format junit --engine sfge --target test/code-fixtures/projects/sfge-smoke-app/src --projectdir test/code-fixtures/projects/sfge-smoke-app/src --outfile smoke-test-results/sfca-pathless.xml
echo "==== Run Salesforce Graph Engine's DFA rules against a folder ===="
$EXE_NAME scanner:run:dfa --format junit --target test/code-fixtures/projects/sfge-smoke-app/src --projectdir test/code-fixtures/projects/sfge-smoke-app/src --outfile smoke-test-results/sfca-dfa.xml
echo "==== Add a JAR of custom rules ===="
$EXE_NAME scanner:rule:add --language apex --path test/test-jars/apex/testjar1.jar
echo "==== List the rules, including the custom ones ===="
$EXE_NAME scanner:rule:list --engine pmd
echo "==== Describe a custom rule ===="
$EXE_NAME scanner:rule:describe -n fakerule1
echo "==== Run a custom rule ===="
$EXE_NAME scanner:run --format junit --category SomeCat1,Security --target test/code-fixtures/projects/app/force-app --outfile smoke-test-results/pmd-custom-rules.xml
echo "==== Remove a custom rule ===="
$EXE_NAME scanner:rule:remove --path test/test-jars/apex/testjar1.jar --force
echo "==== List the rules a final time, to make sure nothing broke ===="
$EXE_NAME scanner:rule:list
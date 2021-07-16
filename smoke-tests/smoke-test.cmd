@echo off
REM Auto-generated on Fri Jul 16 2021
REM This script smoke-tests the entire plugin by running a series of commands that collectively capture a vertical slice
REM of the plugin, hitting every major piece of functionality. If they all succeed, then we can reasonably assume that
REM the plugin is approximately stable.
REM DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/SmokeTestGenerator.js AND RERUN THAT SCRIPT
REM FROM THE PROJECT ROOT!
SET EXE_NAME=%1
echo "==== List all rules w/out filters ===="
call %EXE_NAME% scanner:rule:list || exit /b
echo "==== Filter rules by engine ===="
call %EXE_NAME% scanner:rule:list --engine eslint || exit /b
echo "==== Describe a real rule ===="
call %EXE_NAME% scanner:rule:describe -n EmptyCatchBlock || exit /b
echo "==== Describe a non-existent rule ===="
call %EXE_NAME% scanner:rule:describe -n NotAnActualRule || exit /b
echo "==== Run rules against force-app, which should hit PMD and ESLint engines ===="
call %EXE_NAME% scanner:run --format junit --target test\code-fixtures\projects\app\force-app --outfile smoke-test-results\run1.xml || exit /b
echo "==== Run rules against a typescript file, which should run ESLint-Typescript ===="
call %EXE_NAME% scanner:run --format junit --target test\code-fixtures\projects\ts\src\simpleYetWrong.ts --tsconfig test\code-fixtures\projects\tsconfig.json --outfile smoke-test-results\run2.xml || exit /b
echo "==== Run RetireJS against a folder ===="
call %EXE_NAME% scanner:run --format junit --engine retire-js --target test\code-fixtures\projects\dep-test-app\folder-a --outfile smoke-test-results\run3.xml || exit /b
echo "==== Add a JAR of custom rules ===="
call %EXE_NAME% scanner:rule:add --language apex --path test\test-jars\apex\testjar1.jar || exit /b
echo "==== List the rules, including the custom ones ===="
call %EXE_NAME% scanner:rule:list --engine pmd || exit /b
echo "==== Describe a custom rule ===="
call %EXE_NAME% scanner:rule:describe -n fakerule1 || exit /b
echo "==== Run a custom rule ===="
call %EXE_NAME% scanner:run --format junit --category SomeCat1,Security --target test\code-fixtures\projects\app\force-app --outfile smoke-test-results\run4.xml || exit /b
echo "==== Remove a custom rule ===="
call %EXE_NAME% scanner:rule:remove --path test\test-jars\apex\testjar1.jar --force || exit /b
echo "==== List the rules a final time, to make sure nothing broke ===="
call %EXE_NAME% scanner:rule:list || exit /b
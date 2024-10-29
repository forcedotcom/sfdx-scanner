#!/bin/bash
# Auto-generated on Fri Oct 18 2024
# This script performs a smoke test of the entire plugin by running a series of commands that collectively
# hit a vertical slice of all major features. If they all succeed, then we can reasonably assume that the plugin is
# approximately stable.
# DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/smoke-test-generator.js AND RERUN THAT
# SCRIPT FROM THE PROJECT ROOT!
set -e
EXE_NAME=$1
echo "====== SETUP FOR SMOKE TESTS ======"
echo "====== Delete a a pre-existing smoke-test-results directory ======"
rm -rf smoke-test-results
echo "====== Create smoke-test-results directory ======"
mkdir -p smoke-test-results


echo "====== SMOKE TESTS FOR CONFIG COMMAND ======"
echo "====== Show default config for all engines ======"
$EXE_NAME code-analyzer config
echo "====== Show default config for PMD only ======"
$EXE_NAME code-analyzer config -r pmd
echo "====== Write PMD's default config to a file ======"
$EXE_NAME code-analyzer config -r pmd -f smoke-test-results/pmd-only-config.yml
echo "====== Show configuration from last step's output file ======"
$EXE_NAME code-analyzer config -c smoke-test-results/pmd-only-config.yml
echo "====== Show configuration from pre-existing config file ======"
$EXE_NAME code-analyzer config -c smoke-tests/test-data/config-files/existing-config.yml


echo "====== SMOKE TESTS FOR RULES COMMAND ======"
echo "====== List all rules ======"
$EXE_NAME code-analyzer rules
echo "====== List ESLint rules only ======"
$EXE_NAME code-analyzer rules -r eslint
echo "====== List RetireJS rules only ======"
$EXE_NAME code-analyzer rules -r retire-js
echo "====== List rules relevant to apex-only workspace ======"
$EXE_NAME code-analyzer rules -w smoke-tests/test-data/workspace-with-apex-files
echo "====== List rules matching a nonsensical selector (i.e. list no rules) ======"
$EXE_NAME code-analyzer rules -r asdfasdfasdf
echo "====== List rule overridden in config file ======"
$EXE_NAME code-analyzer rules -r no-unsafe-assignment -c smoke-tests/test-data/config-files/existing-config.yml


echo "====== SMOKE TESTS FOR RUN COMMAND ======"
echo "====== Run all rules against a folder ======"
$EXE_NAME code-analyzer run -w smoke-tests/test-data/workspace-with-mixed-files
echo "====== Run all rules against a file ======"
$EXE_NAME code-analyzer run -w smoke-tests/test-data/workspace-with-mixed-files/my-script.ts
echo "====== Run all rules against a folder and write to outfiles ======"
$EXE_NAME code-analyzer run -w smoke-tests/test-data/workspace-with-apex-files -f smoke-test-results/outfile.json -f smoke-test-results/outfile.html
echo "====== Run a selection of rules against a folder ======"
$EXE_NAME code-analyzer run -r regex -w smoke-tests/test-data/workspace-with-mixed-files
echo "====== Run rules using a config file with overrides ======"
$EXE_NAME code-analyzer run -c smoke-tests/test-data/config-files/existing-config.yml -w smoke-tests/test-data/workspace-with-mixed-files


echo "====== CONCLUSION ======"
echo "If you are seeing this message, the smoke tests ran successfully, and all is (approximately) well"
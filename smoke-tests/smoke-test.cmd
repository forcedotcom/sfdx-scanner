@echo off
REM Auto-generated on Fri Oct 18 2024
REM This script performs a smoke test of the entire plugin by running a series of commands that collectively
REM hit a vertical slice of all major features. If they all succeed, then we can reasonably assume that the plugin is
REM approximately stable.
REM DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/smoke-test-generator.js AND RERUN THAT
REM SCRIPT FROM THE PROJECT ROOT!
SET EXE_NAME=%1
echo "====== SETUP FOR SMOKE TESTS ======"
echo "====== Delete a a pre-existing smoke-test-results directory ======"
if exist smoke-test-results rmdir /s /q smoke-test-results || exit /b 1
echo "====== Create smoke-test-results directory ======"
if not exist smoke-test-results mkdir smoke-test-results || exit /b 1


echo "====== SMOKE TESTS FOR CONFIG COMMAND ======"
echo "====== Show default config for all engines ======"
call %EXE_NAME% code-analyzer config || exit /b 1
echo "====== Show default config for PMD only ======"
call %EXE_NAME% code-analyzer config -r pmd || exit /b 1
echo "====== Write PMD's default config to a file ======"
call %EXE_NAME% code-analyzer config -r pmd -f smoke-test-results/pmd-only-config.yml || exit /b 1
echo "====== Show configuration from last step's output file ======"
call %EXE_NAME% code-analyzer config -c smoke-test-results/pmd-only-config.yml || exit /b 1
echo "====== Show configuration from pre-existing config file ======"
call %EXE_NAME% code-analyzer config -c smoke-tests/test-data/config-files/existing-config.yml || exit /b 1


echo "====== SMOKE TESTS FOR RULES COMMAND ======"
echo "====== List all rules ======"
call %EXE_NAME% code-analyzer rules || exit /b 1
echo "====== List ESLint rules only ======"
call %EXE_NAME% code-analyzer rules -r eslint || exit /b 1
echo "====== List RetireJS rules only ======"
call %EXE_NAME% code-analyzer rules -r retire-js || exit /b 1
echo "====== List rules relevant to apex-only workspace ======"
call %EXE_NAME% code-analyzer rules -w smoke-tests/test-data/workspace-with-apex-files || exit /b 1
echo "====== List rules matching a nonsensical selector (i.e. list no rules) ======"
call %EXE_NAME% code-analyzer rules -r asdfasdfasdf || exit /b 1
echo "====== List rule overridden in config file ======"
call %EXE_NAME% code-analyzer rules -r no-unsafe-assignment -c smoke-tests/test-data/config-files/existing-config.yml || exit /b 1


echo "====== SMOKE TESTS FOR RUN COMMAND ======"
echo "====== Run all rules against a folder ======"
call %EXE_NAME% code-analyzer run -w smoke-tests/test-data/workspace-with-mixed-files || exit /b 1
echo "====== Run all rules against a file ======"
call %EXE_NAME% code-analyzer run -w smoke-tests/test-data/workspace-with-mixed-files/my-script.ts || exit /b 1
echo "====== Run all rules against a folder and write to outfiles ======"
call %EXE_NAME% code-analyzer run -w smoke-tests/test-data/workspace-with-apex-files -f smoke-test-results/outfile.json -f smoke-test-results/outfile.html || exit /b 1
echo "====== Run a selection of rules against a folder ======"
call %EXE_NAME% code-analyzer run -r regex -w smoke-tests/test-data/workspace-with-mixed-files || exit /b 1
echo "====== Run rules using a config file with overrides ======"
call %EXE_NAME% code-analyzer run -c smoke-tests/test-data/config-files/existing-config.yml -w smoke-tests/test-data/workspace-with-mixed-files || exit /b 1


echo "====== CONCLUSION ======"
echo "If you are seeing this message, the smoke tests ran successfully, and all is (approximately) well"
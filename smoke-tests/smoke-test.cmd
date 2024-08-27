@echo off
REM Auto-generated on Tue Aug 27 2024
REM This script WILL EVENTUALLY run a smoke test of the entire plugin by running a series of commands that collectively
REM hit a vertical slice of all major features. If they all succeed, then we can reasonably assume that the plugin is
REM approximately stable.
REM DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/smoke-test-generator.js AND RERUN THAT
REM SCRIPT FROM THE PROJECT ROOT!
SET EXE_NAME=%1
echo "At this point in the alpha, the smoke tests are no-ops and it is fine."
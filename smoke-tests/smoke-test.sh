#!/bin/bash
# Auto-generated on Wed Aug 07 2024
# This script WILL EVENTUALLY run a smoke test of the entire plugin by running a series of commands that collectively
# hit a vertical slice of all major features. If they all succeed, then we can reasonably assume that the plugin is
# approximately stable.
# DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/smoke-test-generator.js AND RERUN THAT
# SCRIPT FROM THE PROJECT ROOT!
set -e
EXE_NAME=$1
undefined
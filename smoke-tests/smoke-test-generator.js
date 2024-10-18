const fs = require('fs');
const path = require('path');

console.log('Switching to smoke test directory');
process.chdir(__dirname);


function generateScriptHeader(isBash) {
	// Both bash and cmd scripts have a standard for what the first line should be.
	let header = isBash ? '#!/bin/bash\n' : '@echo off\n';

	// Bash and cmd have diferent ways of declaring comments.
	const cmt = isBash ? '#' : 'REM';

	// All scripts should start with the same boilerplate disclaimer.
	header += `${cmt} Auto-generated on ${new Date(Date.now()).toDateString()}
${cmt} This script performs a smoke test of the entire plugin by running a series of commands that collectively
${cmt} hit a vertical slice of all major features. If they all succeed, then we can reasonably assume that the plugin is
${cmt} approximately stable.
${cmt} DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/smoke-test-generator.js AND RERUN THAT
${cmt} SCRIPT FROM THE PROJECT ROOT!\n`;

	// Bash has a flag that can be set to make the script exit on the first error.
	if (isBash) {
		header += 'set -e\n';
	}

	// Bash and cmd have different ways of declaring variables.
	header += isBash ? 'EXE_NAME=$1\n' : 'SET EXE_NAME=%1\n';

	return header;
}

function generateScriptBody(isBash, delim) {
	const exeName = isBash ? '$EXE_NAME' : '%EXE_NAME%';


	const commands = [
		// Log a header.
		`echo "====== SETUP FOR SMOKE TESTS ======"`,
		// Delete the smoke test result directory if it already exists
		`echo "====== Delete a a pre-existing smoke-test-results directory ======"`,
		isBash ? 'rm -rf smoke-test-results' : 'if exist smoke-test-results rmdir /s /q smoke-test-results || exit /b 1',
		// Create the output directory
		`echo "====== Create smoke-test-results directory ======"`,
		isBash ? 'mkdir -p smoke-test-results' : 'if not exist smoke-test-results mkdir smoke-test-results || exit /b 1',
		`\n`,
		`echo "====== SMOKE TESTS FOR CONFIG COMMAND ======"`,
		// Show the default configuration for all engines
		`echo "====== Show default config for all engines ======"`,
		`${exeName} code-analyzer config`,
		// Show the default configuration for only one engine
		`echo "====== Show default config for PMD only ======"`,
		`${exeName} code-analyzer config -r pmd`,
		// Write the default config for PMD to a file
		`echo "====== Write PMD's default config to a file ======"`,
		`${exeName} code-analyzer config -r pmd -f ${path.join('.', 'smoke-test-results', 'pmd-only-config.yml')}`,
		// Show the configuration from the config file we just created
		`echo "====== Show configuration from last step's output file ======"`,
		`${exeName} code-analyzer config -c ${path.join('.', 'smoke-test-results', 'pmd-only-config.yml')}`,
		`echo "====== Show configuration from pre-existing config file ======"`,
		`${exeName} code-analyzer config -c ${path.join('.', 'smoke-tests', 'test-data', 'config-files', 'existing-config.yml')}`,
		`\n`,
		`echo "====== SMOKE TESTS FOR RULES COMMAND ======"`,
		// List all rules
		`echo "====== List all rules ======"`,
		`${exeName} code-analyzer rules`,
		// List only rules from ESLint
		`echo "====== List ESLint rules only ======"`,
		`${exeName} code-analyzer rules -r eslint`,
		// List only rules from RetireJS
		`echo "====== List RetireJS rules only ======"`,
		`${exeName} code-analyzer rules -r retire-js`,
		// List only rules that are relevant to a provided workspace
		`echo "====== List rules relevant to apex-only workspace ======"`,
		`${exeName} code-analyzer rules -w ${path.join('.', 'smoke-tests', 'test-data', 'workspace-with-apex-files')}`,
		// List only rules that match nonsensical selector (i.e., list no rules)
		`echo "====== List rules matching a nonsensical selector (i.e. list no rules) ======"`,
		`${exeName} code-analyzer rules -r asdfasdfasdf`,
		// List one rule using a config with overrides
		`echo "====== List rule overridden in config file ======"`,
		`${exeName} code-analyzer rules -r no-unsafe-assignment -c ${path.join('.', 'smoke-tests', 'test-data', 'config-files', 'existing-config.yml')}`,
		'\n',
		`echo "====== SMOKE TESTS FOR RUN COMMAND ======"`,
		// Run all rules against a folder
		`echo "====== Run all rules against a folder ======"`,
		`${exeName} code-analyzer run -w ${path.join('.', 'smoke-tests', 'test-data', 'workspace-with-mixed-files')}`,
		// Run all rules against a file
		`echo "====== Run all rules against a file ======"`,
		`${exeName} code-analyzer run -w ${path.join('.', 'smoke-tests', 'test-data', 'workspace-with-mixed-files', 'my-script.ts')}`,
		// Run all rules against a folder and write the output to some files
		`echo "====== Run all rules against a folder and write to outfiles ======"`,
		`${exeName} code-analyzer run -w ${path.join('.', 'smoke-tests', 'test-data', 'workspace-with-apex-files')} -f ${path.join('.', 'smoke-test-results', 'outfile.json')} -f ${path.join('.', 'smoke-test-results', 'outfile.html')}`,
		// Run a selection of rules against a folder
		`echo "====== Run a selection of rules against a folder ======"`,
		`${exeName} code-analyzer run -r regex -w ${path.join('.', 'smoke-tests', 'test-data', 'workspace-with-mixed-files')}`,
		// Run rules using a config file with overrides
		`echo "====== Run rules using a config file with overrides ======"`,
		`${exeName} code-analyzer run -c ${path.join('.', 'smoke-tests', 'test-data', 'config-files', 'existing-config.yml')} -w ${path.join('.', 'smoke-tests', 'test-data', 'workspace-with-mixed-files')}`,
		'\n',
		`echo "====== CONCLUSION ======"`,
		`echo "If you are seeing this message, the smoke tests ran successfully, and all is (approximately) well"`
	];

	// In a cmd script, you need to prepend plugin commands with "call" in order to make sure that the script continues,
	// and you need to postfix it with another snippet to make it actually exist when an error is encountered.
	if (!isBash) {
		for (let i = 0; i < commands.length; i++) {
			if (commands[i].startsWith(exeName)) {
				commands[i] = `call ${commands[i]} || exit /b 1`
			}
		}
	}

	// Combine the commands and return the script body.
	return commands.join('\n');
}

function generateScript(isBash, delim) {
	let header = generateScriptHeader(isBash);
	let body = generateScriptBody(isBash, delim);
	return header + body;
}

// We need the following set of smoke tests:
// A Bash script that can be run in POSIX or in Windows's bash.exe.
console.log(`generating smoke-test.sh...`);
fs.writeFileSync('smoke-test.sh', generateScript(true, '/'));
console.log(`smoke-test.sh generated.`);
// A .cmd script that can be run in Powershell or the Windows Command Prompt.
console.log(`generating smoke-test.cmd...`);
fs.writeFileSync('smoke-test.cmd', generateScript(false, '\\'));
console.log(`smoke-test.cmd generated`);

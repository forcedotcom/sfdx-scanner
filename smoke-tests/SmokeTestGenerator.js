/**
 * This script auto-generates bash/cmd scripts meant to test smoke test the entire plug-in. These scripts will do so by
 * running a series of operations that collectively capture a vertical slice of the plug-in, hitting every major piece
 * of functionality. If they all succeed, then we can conclude that the plug-in is approximately stable.
 * Invoke from the project root folder, e.g.
 * `node smoke-tests/SmokeTestGenerator.js`
 *
 * @author Josh Feingold
 */

const fs = require('fs');
const path = require('path');


function generateScriptHeader(isBash) {
	// Both bash and cmd scripts have a standard for what the first line should be.
	let header = isBash ? '#!/bin/bash\n' : '@echo off\n';

	// Bash and cmd have different ways of declaring comments.
	const cmt = isBash ? '#' : 'REM';

	// All scripts should start with the same boilerplate disclaimer.
	header += `${cmt} Auto-generated on ${new Date(Date.now()).toDateString()}
${cmt} This script smoke-tests the entire plug-in by running a series of commands that collectively capture a vertical slice
${cmt} of the plug-in, hitting every major piece of functionality. If they all succeed, then we can reasonably assume that
${cmt} the plug-in is approximately stable.
${cmt} DO NOT EDIT THIS SCRIPT DIRECTLY! INSTEAD, MAKE CHANGES IN ./smoke-tests/SmokeTestGenerator.js AND RERUN THAT SCRIPT
${cmt} FROM THE PROJECT ROOT!\n`;

	// Bash has a flag that can be set to make the script exit on the first error.
	if (isBash) {
		header += `set -e\n`;
	}

	// Bash and cmd have different ways of declaring variables.
	header += isBash ? 'EXE_NAME=$1\n' : 'SET EXE_NAME=%1\n';

	return header;
}

function buildPath(pathSegs, delim) {
	return pathSegs.join(delim);
}

function generateScriptBody(isBash, delim) {
	const projectsPath = ['test', 'code-fixtures', 'projects'];
	const customRulePath = ['test', 'test-jars', 'apex', 'testjar1.jar'];
	const customConfigPath = ['test', 'test-xml', 'category', 'apex', 'smoke-config.xml'];
	const resultsPath = ['smoke-test-results'];

	const exeName = isBash ? '$EXE_NAME' : '%EXE_NAME%';

	// Declare an array with all of the commands we intend to execute.
	const commands = [
		// Log a header.
		`echo "====== STARTING SMOKE TEST ======"`,
		// Create the results directory.
		`echo "==== Make results directory ===="`,
		isBash ? `mkdir -p smoke-test-results` : `if not exist smoke-test-results mkdir smoke-test-results || exit /b 1`,
		// List all the rules w/out filters.
		`echo "==== List all rules w/out filters ===="`,
		`${exeName} scanner:rule:list`,
		// List rules, filtered by engine.
		`echo "==== Filter rules by engine ===="`,
		`${exeName} scanner:rule:list --engine eslint`,
		// Describe a real rule.
		`echo "==== Describe a real rule ===="`,
		`${exeName} scanner:rule:describe -n EmptyCatchBlock`,
		// Describe a non-existent rule.
		`echo "==== Describe a non-existent rule ===="`,
		`${exeName} scanner:rule:describe -n NotAnActualRule`,
		// Run such that PMD and ESLint are invoked.
		`echo "==== Run rules against force-app, which should hit PMD and ESLint engines ===="`,
		`${exeName} scanner:run --format junit --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --outfile ${buildPath([...resultsPath, 'pmd-eslint-result.xml'], delim)}`,
		// Run such that ESLint-Typescript is invoked.
		`echo "==== Run rules against a typescript file, which should run ESLint-Typescript ===="`,
		`${exeName} scanner:run --format junit --target ${buildPath([...projectsPath, 'ts', 'src', 'simpleYetWrong.ts'], delim)} --tsconfig ${buildPath([...projectsPath, 'tsconfig.json'], delim)} --outfile ${buildPath([...resultsPath, 'eslint-typescript-result.xml'], delim)}`,
		// Run such that RetireJS is invoked.
		`echo "==== Run RetireJS against a folder ===="`,
		`${exeName} scanner:run --format junit --engine retire-js --target ${buildPath([...projectsPath, 'dep-test-app', 'folder-a'], delim)} --outfile ${buildPath([...resultsPath, 'retire-js-result.xml'], delim)}`,
		// Run such that CPD is invoked.
		`echo "==== Run CPD against a folder ===="`,
		`${exeName} scanner:run --format junit --engine cpd --target ${buildPath([...projectsPath, 'cpd-test-app', 'src', 'classes'], delim)} --outfile ${buildPath([...resultsPath, 'cpd-result.xml'], delim)}`,
		// Run such that PMD is invoked with a custom config.
		`echo "==== Run PMD with custom config via --pmdconfig flag ===="`,
		`${exeName} scanner:run --format junit --engine pmd --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --pmdconfig ${buildPath(customConfigPath, delim)} --outfile ${buildPath([...resultsPath, 'pmd-customconfig-result.xml'], delim)}`,
		// Run such that GraphEngine's AST-based rules are invoked.
		`echo "==== Run Salesforce Graph Engine's non-DFA rules against a folder ===="`,
		`${exeName} scanner:run --format junit --engine sfge --target ${buildPath([...projectsPath, 'sfge-smoke-app', 'src'], delim)} --projectdir ${buildPath([...projectsPath, 'sfge-smoke-app', 'src'], delim)} --outfile ${buildPath([...resultsPath, 'sfca-pathless-result.xml'], delim)}`,
		// Run such that the DFA rules are invoked.
		`echo "==== Run Salesforce Graph Engine's DFA rules against a folder ===="`,
		`${exeName} scanner:run:dfa --format junit --target ${buildPath([...projectsPath, 'sfge-smoke-app', 'src'], delim)} --projectdir ${buildPath([...projectsPath, 'sfge-smoke-app', 'src'], delim)} --outfile ${buildPath([...resultsPath, 'sfca-dfa-result.xml'], delim)}`,
		// Add a JAR of custom rules.
		`echo "==== Add a JAR of custom rules ===="`,
		`${exeName} scanner:rule:add --language apex --path ${buildPath(customRulePath, delim)}`,
		// List the rules, which should now include the custom ones.
		`echo "==== List the rules, including the custom ones ===="`,
		`${exeName} scanner:rule:list --engine pmd`,
		// Describe one of the custom rules.
		`echo "==== Describe a custom rule ===="`,
		`${exeName} scanner:rule:describe -n fakerule1`,
		// Run such that the custom rules are executed.
		`echo "==== Run a custom rule ===="`,
		`${exeName} scanner:run --format junit --category SomeCat1,Security --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --outfile ${buildPath([...resultsPath, 'pmd-custom-rules-result.xml'], delim)}`,
		// Remove the custom rules.
		`echo "==== Remove a custom rule ===="`,
		`${exeName} scanner:rule:remove --path ${buildPath(customRulePath, delim)} --force`,
		// List the rules one last time.
		`echo "==== List the rules a final time, to make sure nothing broke ===="`,
		`${exeName} scanner:rule:list`
	];

	// In a cmd script, you need to prepend plug-in commands with "call" in order to make sure that the script continues,
	// and you need to postfix it with another snippet to make it actually exit when an error is encountered.
	if (!isBash) {
		for (let i = 4; i < commands.length; i += 2) {
			commands[i] = "call " + commands[i] + " || exit /b 1";
		}
	}

	// Combine the commands together and return the script body.
	return commands.join('\n');
}

function generateScript(isBash, delim) {
	let header = generateScriptHeader(isBash);
	let body = generateScriptBody(isBash, delim);

	return header + body;
}


// We need the following set of smoke tests:
// A Bash script that can be run in POSIX or in Windows' bash.exe.
fs.writeFileSync(path.join('smoke-tests', 'smoke-test.sh'), generateScript(true, '/'));
// A .cmd script that can be run in Powershell or the Windows command prompt.
fs.writeFileSync(path.join('smoke-tests', 'smoke-test.cmd'), generateScript(false, '\\'));


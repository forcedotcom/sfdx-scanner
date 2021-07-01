/**
 * This script auto-generates bash/cmd scripts meant to test smoke test the entire plugin. These scripts will do so by
 * running a series of operations that collectively capture a vertical slice of the plugin, hitting every major piece
 * of functionality. If they all succeed, then we can conclude that the plugin is approximately stable.
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
${cmt} This script smoke-tests the entire plugin by running a series of commands that collectively capture a vertical slice
${cmt} of the plugin, hitting every major piece of functionality. If they all succeed, then we can reasonably assume that
${cmt} the plugin is approximately stable.
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
	const resultsPath = ['test-results'];

	const exeName = isBash ? '$EXE_NAME' : '%EXE_NAME%';

	// Declare an array with all of the commands we intend to execute.
	const commands = [
		`echo "==== List all rules w/out filters ===="`,
		`${exeName} scanner:rule:list`,
		`echo "==== Filter rules by engine ===="`,
		`${exeName} scanner:rule:list --engine eslint`,
		`echo "==== Describe a real rule ===="`,
		`${exeName} scanner:rule:describe -n EmptyCatchBlock`,
		`echo "==== Describe a non-existent rule ===="`,
		`${exeName} scanner:rule:describe -n NotAnActualRule`,
		`echo "==== Run rules against force-app, which should hit PMD and ESLint engines ===="`,
		`${exeName} scanner:run --format junit --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --outfile ${buildPath([...resultsPath, 'run1.xml'], delim)}`,
		`echo "==== Run rules against a typescript file, which should run ESLint-Typescript ===="`,
		`${exeName} scanner:run --format junit --target ${buildPath([...projectsPath, 'ts', 'src', 'simpleYetWrong.ts'], delim)} --tsconfig ${buildPath([...projectsPath, 'tsconfig.json'], delim)} --outfile ${buildPath([...resultsPath, 'run2.xml'], delim)}`,
		`echo "==== Run RetireJS against a folder ===="`,
		`${exeName} scanner:run --format junit --engine retire-js --target ${buildPath([...projectsPath, 'dep-test-app', 'folder-a'], delim)} --outfile ${buildPath([...resultsPath, 'run3.xml'], delim)}`,
		`echo "==== Add a JAR of custom rules ===="`,
		`${exeName} scanner:rule:add --language apex --path ${buildPath(customRulePath, delim)}`,
		`echo "==== List the rules, including the custom ones ===="`,
		`${exeName} scanner:rule:list --engine pmd`,
		`echo "==== Describe a custom rule ===="`,
		`${exeName} scanner:rule:describe -n fakerule1`,
		`echo "==== Run a custom rule ===="`,
		`${exeName} scanner:run --format junit --category SomeCat1,Security --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --outfile ${buildPath([...resultsPath, 'run4.xml'], delim)}`,
		`echo "==== Remove a custom rule ===="`,
		`${exeName} scanner:rule:remove --path ${buildPath(customRulePath, delim)} --force`,
		`echo "==== List the rules a final time, to make sure nothing broke ===="`,
		`${exeName} scanner:rule:list`
	];

	// In a cmd script, the only reliable way to exit after an error is to append an extra statement to every line.
//	if (!isBash) {
//		for (let i = 1; i < commands.length; i += 2) {
//			// This little snippet should only be executed if the command fails.
//			commands[i] += ' || exit';
//		}
//	}

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
fs.writeFileSync(path.join('smoke-tests', 'bash.sh'), generateScript(true, '/'));
// A .cmd script that can be run in Powershell or the Windows command prompt.
fs.writeFileSync(path.join('smoke-tests', 'powershell.cmd'), generateScript(false, '\\'));


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
	// Bash scripts require a special header.
	let header = isBash ? '#!/bin/bash\n' : "";

	header += `# Auto-generated on ${new Date(Date.now()).toDateString()}
#
# This script smoke-tests the entire plugin by running a series of commands that collectively capture a
# vertical slice of the plugin, hitting every major piece of functionality. If they all succeed, we can reasonably
# conclude that the plugin is approximately stable.
#
# DO NOT EDIT THIS SCRIPT DIRECTLY!
# INSTEAD, MAKE CHANGES IN 	./SmokeTestGenerator.js, 

`;

	header += isBash ? 'set -e\n' : "";
	return header;
}

function buildPath(pathSegs, delim) {
	return pathSegs.join(delim);
}

function generateScriptContent(delim) {
	const projectsPath = ['test', 'code-fixtures', 'projects'];
	const customRulePath = ['test', 'test-jars', 'apex', 'testjar1.jar'];
	const resultsPath = ['test-results'];

	return `echo "List all rules w/out filters"
$1 scanner:rule:list
echo "Filter rules by engine"
$1 scanner:rule:list --engine eslint
echo "Describe a real rule"
$1 scanner:rule:describe -n EmptyCatchBlock
echo "Describe a non-existent rule"
$1 scanner:rule:describe -n NotAnActualRule
echo "Run rules against force-app, which should hit PMD and ESLint engines"
$1 scanner:run --format junit --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --outfile ${buildPath([...resultsPath, 'run1.xml'], delim)}
echo "Run rules against a typescript file, which should run ESLint-Typescript"
$1 scanner:run --format junit --target ${buildPath([...projectsPath, 'ts', 'src', 'simpleYetWrong.ts'], delim)} --tsconfig ${buildPath([...projectsPath, 'tsconfig.json'], delim)} --outfile ${buildPath([...resultsPath, 'run2.xml'], delim)}
echo "Run RetireJS against a folder"
$1 scanner:run --format junit --engine retire-js --target ${buildPath([...projectsPath, 'dep-test-app', 'folder-a'], delim)} --outfile ${buildPath([...resultsPath, 'run3.xml'], delim)}
echo "Add a JAR of custom rules"
$1 scanner:rule:add --language apex --path ${buildPath(customRulePath, delim)}
echo "List the rules, including the custom ones"
$1 scanner:rule:list --engine pmd
echo "Describe a custom rule"
$1 scanner:rule:describe -n fakerule1
echo "Run a custom rule"
$1 scanner:run --format junit --category "SomeCat1,Security --target ${buildPath([...projectsPath, 'app', 'force-app'], delim)} --outfile ${buildPath([...resultsPath, 'run4.xml'], delim)}
echo "Remove a custom rule"
$1 scanner:rule:remove --path ${buildPath(customRulePath, delim)} --force
echo "List the rules a final time, to make sure nothing broke"
$1 scanner:rule:list`;
}


// We need the following set of smoke tests:
// Bash scripts for both POSIX and Windows that run a local version of the plugin.
fs.writeFileSync(path.join('smoke-tests', 'bash-posix.sh'), generateScriptHeader(true) + generateScriptContent('/'));
fs.writeFileSync(path.join('smoke-tests', 'bash-windows.sh'), generateScriptHeader(true) + generateScriptContent('\\'));


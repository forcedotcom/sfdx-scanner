const fs = require('fs');

console.log('Switching to smoke test directory');
process.chdir(__dirname);


function generateScriptHeader(isBash) {
	// Both bash and cmd scripts have a standard for what the first line should be.
	let header = isBash ? '#!/bin/bash\n' : '@echo off\n';

	// Bash and cmd have diferent ways of declaring comments.
	const cmt = isBash ? '#' : 'REM';

	// All scripts should start with the same boilerplate disclaimer.
	header += `${cmt} Auto-generated on ${new Date(Date.now()).toDateString()}
${cmt} This script WILL EVENTUALLY run a smoke test of the entire plugin by running a series of commands that collectively
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
	const commands = [
		// Log an explainer that since we're in the Alpha stage, we don't have a fully fleshed out smoke test.
		`echo "At this point in the alpha, the smoke tests are no-ops and it is fine."`
	]

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

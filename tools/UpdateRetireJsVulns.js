/**
 * Downloads the latest version of RetireJS's internal catalog of JS vulnerabilities, then writes the resulting JSON to
 * sfdx-scanner/src/lib/retire-js/RetireJsVulns.json.
 *
 * Usage: From the root folder of the project, run `node tools/UpdateRetireJsVulns.js`.
 */


// == IMPORTS AND GLOBALS ==
const URL = require('url');
const https = require('https');
const fsPromises = require('fs').promises;

const RETIREJS_VULNS_PATH = require('path').resolve("retire-js", "RetireJsVulns.json");

loadJson('https://raw.githubusercontent.com/RetireJS/retire.js/master/repository/jsrepository.json')
	.then((repoJson) => {
		console.log(`Successfully downloaded raw RetireJS repo file`);
		validateJson(repoJson);
		console.log(`JSON is valid`);
		cleanUpJson(repoJson);
		console.log(`JSON has been cleaned`);
		return writeJson(repoJson);
	})
	.then(() => {
		console.log(`Successfully wrote RetireJS vulnerability catalog to ${RETIREJS_VULNS_PATH}`);
	}, (err) => {
		console.log(`Error creating catalog: ${err.message | err}`);
	});


function loadJson(repoUrl) {
	// Define the options for the HTTP request.
	const requestOptions = Object.assign({}, URL.parse(repoUrl), {method: 'GET'});

	return new Promise((resolve, reject) => {
		// Make the HTTP request.
		const req = https.request(requestOptions, (res) => {
			// A status code other than 200 means a failure. Reject the promise.
			if (res.statusCode !== 200) {
				return reject(`Error downloading ${repoUrl}. Status ${res.statusCode}; ${res.statusMessage}`);
			}
			// Otherwise, create a data buffer...
			const dataBuffers = [];
			// ...and populate it with data as it comes in.
			res.on('data', data => dataBuffers.push(data));

			// Once we've got all of the data, parse it and resolve the promise.
			res.on('end', () => {
				resolve(JSON.parse(Buffer.concat(dataBuffers).toString()));
			});
		});
		// On an error, reject the promise.
		req.on('error', e => reject(`Error downloading ${repoUrl}: ${e.toString()}`));
		req.end();
	});
}

function validateJson(repoJson) {
	// Create an array to track problems.
	const problems = [];
	// Iterate over every key in the JSON.
	for (const key of Object.keys(repoJson)) {
		const value = repoJson[key];
		// If the key has no vulnerabilities, we can just skip it.
		if (!value.vulnerabilities || value.vulnerabilities.length === 0) {
			continue;
		}
		// If there are vulnerabilities, make sure they have all the attributes we need.
		for (let i = 0; i < value.vulnerabilities.length; i++) {
			const vuln = value.vulnerabilities[i];
			if (!vuln.identifiers) {
				problems.push(`Component: ${key}. Problem: Vulnerability #${i + 1} lacks identifiers.`);
			}
			if (!vuln.severity) {
				problems.push(`Component: ${key}. Problem: Vulnerability #${i + 1} lacks a severity.`);
			}
		}
	}
	// If there are any problems, throw an error including all of them.
	if (problems.length > 0) {
		throw new Error(problems.join('\n'));
	}
}

function cleanUpJson(repoJson) {
	for (const key of Object.keys(repoJson)) {
		if (repoJson[key].extractors.func) {
			console.log(`Removing property: ${key}.extractors.func`);
			delete repoJson[key].extractors.func;
		}
	}
}

function writeJson(repoJson) {
	return fsPromises.writeFile(RETIREJS_VULNS_PATH, JSON.stringify(repoJson, null, 2));
}

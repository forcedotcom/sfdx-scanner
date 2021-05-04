// == IMPORTS AND GLOBALS ==
const URL = require('url');
const https = require('https');
const fsPromises = require('fs').promises;

const RETIREJS_VULNS_PATH = require('path').resolve("src", "lib", "retire-js", "RetireJsVulns.json");


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

loadJson('https://raw.githubusercontent.com/RetireJS/retire.js/master/repository/jsrepository.json')
	.then((repoJson) => {
		console.log(`Successfully downloaded raw RetireJS repo file`);
		// Sanitize the JSON by deleting all of the `func` attributes.
		for (const key of Object.keys(repoJson)) {
			if (repoJson[key].extractors.func) {
				console.log(`Removing property: ${key}.extractors.func`);
				delete repoJson[key].extractors.func;
			}
		}

		return fsPromises.writeFile(RETIREJS_VULNS_PATH, JSON.stringify(repoJson, null, 2));
	})
	.then(() => {
		console.log(`Successfully wrote RetireJS vulnerability catalog to ${RETIREJS_VULNS_PATH}`);
	}, (err) => {
		console.log(`Error creating catalog: ${err.message | err}`);
	});

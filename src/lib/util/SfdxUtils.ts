import childProcess = require('child_process');

/**
 * A variable to store SFDX's version number, so we don't have to keep running `sfdx -v`.
 */
let SFDX_VERSION: string;
export async function getSfdxVersion(): Promise<string> {
	// If we already have a cached value, we can just return that instead of doing everything else.
	if (SFDX_VERSION !== undefined) {
		return SFDX_VERSION;
	}

	// Get the output of `sfdx -v`.
	let rawVersionString: string;
	try {
		rawVersionString = await new Promise<string>((res, rej) => {
			childProcess.exec('sfdx -v', (error, stdout, stderr) => {
				if (error) {
					rej(stderr);
				} else {
					res(stdout);
				}
			});
		});
	} catch (e) {
		// If the command fails, then we have no way of determining what the version is.
		// So just set the variable to unknown and be done with it.
		SFDX_VERSION = 'unknown';
		return SFDX_VERSION;
	}

	// The actual output for `sfdx -v` is a long-ish string that has stuff we don't want. So use this regex to just get
	// the SFDX version.
	const regex = /(sfdx-cli\/\d+\.\d+\.\d+)/g;
	const match = regex.exec(rawVersionString);
	if (match.length > 0) {
		SFDX_VERSION = match[0];
	} else {
		// Even if our regex didn't pull out the value that we expected it to, we can still use the results of the command.
		// It'll just be more verbose than is strictly necessary.
		SFDX_VERSION = rawVersionString;
	}
	return SFDX_VERSION;
}

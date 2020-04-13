/**
 *  Code to be evaluated after the scanner plugin is installed.
 */
import JreSetupManager = require('./JreSetupManager');

async function verifyJava(): Promise<void> {
	// Use a try-catch block since a failure here shouldn't tank the entire script.
	try {
		await JreSetupManager.verifyJreSetup();
	} catch (e) {
		// Intentionally left empty, since the script can't really log anything intelligently.
	}
}


export async function execute(): Promise<void> {
	// Run our scripts in a try-catch block so any important errors can bubble up to the top and be logged.
	try {
		// First, verify that Java is setup properly.
		await verifyJava();
	} catch (e) {
		// Intentionally left empty, but should be filled in if intelligent error handling is needed in the future.
	}
}

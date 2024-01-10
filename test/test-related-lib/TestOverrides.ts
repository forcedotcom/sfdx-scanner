import { ENV_VAR_NAMES} from "../../src/Constants";
import os = require('os');
import path = require('path');
import { Controller } from "../../src/Controller";
import { registerAll } from "../../src/ioc.config";

export const TEST_SCANNER_PATH = path.join(os.homedir(), '.sfdx-scanner-test');

const container = Controller.container;

export function initializeTestSetup(): void {
	// Pretty much every test expects to use the Test config folder instead of the real one.
	process.env[ENV_VAR_NAMES.SCANNER_PATH_OVERRIDE] = TEST_SCANNER_PATH;
	container.reset();
	registerAll();
}

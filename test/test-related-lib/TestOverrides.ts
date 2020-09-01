import { EnvOverridable } from "../../src/Constants";
import os = require('os');
import path = require('path');
import { Services } from "../../src/Constants";
import { Controller } from "../../src/Controller";
import { registerAll } from "../../src/ioc.config";

export class TestOverrides implements EnvOverridable {
	public getSfdxScannerPath(): string {
		return path.join(os.homedir(), '.sfdx-scanner-test');
	}
}

const container = Controller.container;

function setupTestAlternatives(): void {
	container.register(Services.EnvOverridable, TestOverrides);
}

export function initializeTestSetup(): void {
	container.reset();
	setupTestAlternatives();
	registerAll();
}

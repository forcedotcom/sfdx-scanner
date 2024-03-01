import fs = require('fs');
import path = require('path');
import {AnyJson} from '@salesforce/ts-types';
import {
	execCmd,
	ExecCmdResult,
	execInteractiveCmd,
	InteractiveCommandExecutionResult, PromptAnswers
} from '@salesforce/cli-plugins-testkit';
// @ts-ignore
import * as TestOverrides from './test-related-lib/TestOverrides';
import Sinon = require('sinon');
import LocalCatalog from '../src/lib/services/LocalCatalog';


const CATALOG_FIXTURE_PATH = path.join('test', 'catalog-fixtures', 'DefaultCatalogFixture.json');
export const CATALOG_FIXTURE_RULE_COUNT = 16;
export const CATALOG_FIXTURE_DEFAULT_ENABLED_RULE_COUNT = 11;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function prettyPrint(obj: any): string {
	return JSON.stringify(obj, null, 2);
}

export function stubCatalogFixture(): void {
	// Make sure all catalogs exist where they're supposed to.
	if (!fs.existsSync(CATALOG_FIXTURE_PATH)) {
		throw new Error('Fake catalog does not exist');
	}

	// Make sure all catalogs have the expected number of rules.
	const catalogJson = JSON.parse(fs.readFileSync(CATALOG_FIXTURE_PATH).toString());
	if (catalogJson.rules.length !== CATALOG_FIXTURE_RULE_COUNT) {
		throw new Error('Fake catalog has ' + catalogJson.rules.length + ' rules instead of ' + CATALOG_FIXTURE_RULE_COUNT);
	}

	// Stub out the LocalCatalog's getCatalog method so it always returns the fake catalog, whose contents are known,
	// and never overwrites the real catalog. (Or we could use the IOC container to do this without sinon.)
	Sinon.stub(LocalCatalog.prototype, 'getCatalog').callsFake(async () => {
		return JSON.parse(fs.readFileSync(CATALOG_FIXTURE_PATH).toString());
	});
}

export function runCommand(command: string): ExecCmdResult<AnyJson> {
	TestOverrides.initializeTestSetup();
	return execCmd(command);
}

export function runInteractiveCommand(command: string, answers: PromptAnswers): Promise<InteractiveCommandExecutionResult> {
	TestOverrides.initializeTestSetup();
	return execInteractiveCmd(command, answers);
}



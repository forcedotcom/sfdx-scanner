import fs = require('fs');
import path = require('path');
import { test } from '@salesforce/command/lib/test';
import * as TestOverrides from './test-related-lib/TestOverrides';
import Sinon = require('sinon');
import LocalCatalog from '../src/lib/services/LocalCatalog';


const CATALOG_FIXTURE_PATH = path.join('test', 'catalog-fixtures', 'DefaultCatalogFixture.json');
export const CATALOG_FIXTURE_RULE_COUNT = 15;
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

/**
 * Initial setup needed by all oclif command unit tests.
 *
 * Example:
 * setupCommandTest
 * 	.command(['scanner:run', '-t', 'test-code'])
 * 	.it('Scanner Run Relative Path Succeeds', ctx => {
 * 		expect(ctx.stdout).to.contain('No rule violations found.');
 * 	});
 */
export const setupCommandTest = test
	.do(() => TestOverrides.initializeTestSetup())
	.stdout()
	.stderr();


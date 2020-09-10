import {test} from '@salesforce/command/lib/test';
import * as TestOverrides from './test-related-lib/TestOverrides';

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
const setupCommandTest = test
	.do(() => TestOverrides.initializeTestSetup())
	.stdout()
	.stderr();

export { setupCommandTest };

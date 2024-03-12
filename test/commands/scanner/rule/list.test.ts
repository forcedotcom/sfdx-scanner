import {expect} from 'chai';
import {runCommand} from '../../../TestUtils';
import {BundleName, getMessage} from "../../../../src/MessageCatalog";

describe('scanner rule list', () => {

	describe('E2E', () => {

		describe('Test Case: Filtering by ruleset only', () => {

			it('--ruleset option shows deprecation warning', () => {
				const output = runCommand(`scanner rule list --ruleset Braces`);
				expect(output.shellOutput.stderr).contains(getMessage(BundleName.List, 'rulesetDeprecation'));
			});
		});
	});
});

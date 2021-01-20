import {expect} from 'chai';
import path = require('path');
import {LWCEslintStrategy} from '../../../src/lib/eslint/LWCEslintStrategy';
import {ProcessRuleViolationType} from '../../../src/lib/eslint/EslintCommons';
import {HARDCODED_RULES} from '../../../src/Constants';

describe('LWCEslintStrategy', () => {
	describe('processRuleViolation()', () => {
		let violationProcessor: ProcessRuleViolationType = null;

		before(async () => {
			const strategy = new LWCEslintStrategy();
			await strategy.init();

			violationProcessor = strategy.processRuleViolation();
		});

		it('Parsing errors are properly handled', () => {
			const fileName = path.join('test', 'code-fixtures', 'invalid-lwc', 'invalidApiDecorator', 'noLeadingUpperCase.js');
			const parserError = {
				"line": 3,
				"column": 6,
				"severity": 2,
				// All of the weird-looking stuff is decorated markup that the LWC plugin returns when parsing fails.
				// We want it gone from the final message.
				"message": "Parsing error: Unexpected token, expected \";\"\n\n\u001b[0m \u001b[90m 1 | \u001b[39m\u001b[36mimport\u001b[39m {api} from \u001b[32m\"lwc\"\u001b[39m\u001b[33m;\u001b[39m\u001b[0m\n\u001b[0m \u001b[90m 2 | \u001b[39m\u001b[0m\n\u001b[0m\u001b[31m\u001b[1m>\u001b[22m\u001b[39m\u001b[90m 3 | \u001b[39mcass \u001b[33mFoo\u001b[39m {\u001b[0m\n\u001b[0m \u001b[90m   | \u001b[39m     \u001b[31m\u001b[1m^\u001b[22m\u001b[39m\u001b[0m\n\u001b[0m \u001b[90m 4 | \u001b[39m\u001b[0m\n\u001b[0m \u001b[90m 5 | \u001b[39m    \u001b[33m@\u001b[39mapi\u001b[0m\n\u001b[0m \u001b[90m 6 | \u001b[39m    foo \u001b[33m=\u001b[39m \u001b[36mtrue\u001b[39m\u001b[33m;\u001b[39m\u001b[0m",
				"ruleName": null,
				"category": "",
				"url": ""
			};
			const originalMsg = parserError.message;

			violationProcessor(fileName, parserError);

			expect(parserError.ruleName).to.equal(HARDCODED_RULES.FILES_MUST_COMPILE.name, 'wrong name assigned');
			expect(parserError.category).to.equal(HARDCODED_RULES.FILES_MUST_COMPILE.category, 'wrong category applied');
			expect(parserError.message).to.equal(originalMsg.split('\n')[0], 'Error message should have been trimmed');
		});
	});
});

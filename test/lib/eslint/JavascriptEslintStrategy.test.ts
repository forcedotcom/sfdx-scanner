import {expect} from 'chai';
import path = require('path');
import {JavascriptEslintStrategy} from '../../../src/lib/eslint/JavascriptEslintStrategy';
import {ProcessRuleViolationType} from '../../../src/lib/eslint/EslintCommons';
import {HARDCODED_RULES} from '../../../src/Constants';

describe('JavascriptEslintStrategy', () => {
	describe('processRuleViolation()', () => {
		let violationProcessor: ProcessRuleViolationType = null;

		before(async () => {
			const strategy = new JavascriptEslintStrategy();
			await strategy.init();

			violationProcessor = strategy.processRuleViolation();
		});

		it('Parsing errors are properly handled', () => {
			const fileName = path.join('test', 'code-fixtures', 'projects', 'js', 'simpleYetWrong.js');
			const parserError = {
				"line": 38,
				"column": 13,
				"severity": 2,
				"message": "Parsing error: Unexpected token ;",
				"ruleName": null,
				"category": "",
				"url": ""
			};

			violationProcessor(fileName, parserError);

			expect(parserError.ruleName).to.equal(HARDCODED_RULES.FILES_MUST_COMPILE.name, 'wrong name assigned');
			expect(parserError.category).to.equal(HARDCODED_RULES.FILES_MUST_COMPILE.category, 'wrong category applied');
		});
	});
});

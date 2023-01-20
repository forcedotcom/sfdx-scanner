import { Rule, RuleGroup, RuleTarget, ESRule } from '../../../src/types';
import {ESLint, Linter} from 'eslint';

const engineName = 'TestHarnessEngine'; // TODO: crude code. revisit

export function getDummyRuleGroup(): RuleGroup {
	return { engine: engineName, name: "Group name", paths: ['/some/random/path'] };
}

export function getDummyRule(myEngineName = engineName): Rule {
	return {
		engine: myEngineName,
		name: "MyTestRule",
		description: "my test rule",
		categories: ["some category"],
		languages: ["language"],
		sourcepackage: "MySourcePackage",
		isDfa: false,
		rulesets: [],
		defaultEnabled: true
	}
}

export function getDummyTarget(isDir = true): RuleTarget {
	return {
		target: "/some/target",
		isDirectory: isDir,
		paths: ['/some/target/path1', '/some/target/path2']
	}
}

export function getDummyEsRuleMap(ruleId = 'ruleId', category = 'myCategory', description = 'my description'): Map<string, ESRule> {
	const map = new Map<string, ESRule>();
	map.set(ruleId, getDummyEsRule(category, description));
	return map;
}

export function getDummyEsRule(category = 'problem', description = 'my description'): ESRule {
	return {
		meta: {
			type: category,
			docs: {
				description: description,
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	}
}

export function getDummyTypescriptRuleMap(): Map<string,ESRule> {
	const dummyMap: Map<string, ESRule> = new Map();
	dummyMap.set('fake-active', {
		meta: {
			type: "problem",
			docs: {
				description: 'Lorem Ipsum',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	});
	dummyMap.set('fake-deprecated', {
		meta: {
			deprecated: true,
			type: "suggestion",
			docs: {
				description: 'Lorem Ipsum',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	});
	dummyMap.set('fake-extended-a', {
		meta: {
			type: "layout",
			docs: {
				description: 'Lorem Ipsum',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	});
	dummyMap.set('fake-extended-b', {
		meta: {
			type: "problem",
			docs: {
				description: 'Lorem Ipsum',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	});
	dummyMap.set('@typescript-eslint/fake-extended-a', {
		meta: {
			type: "layout",
			docs: {
				description: 'Lorem Ipsum',
				recommended: true,
				extendsBaseRule: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	});
	dummyMap.set('@typescript-eslint/renamed-b-extension', {
		meta: {
			type: "problem",
			docs: {
				description: 'Lorem Ipsum',
				recommended: true,
				extendsBaseRule: 'fake-extended-b',
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		/* eslint-disable @typescript-eslint/no-unused-vars */
		create: (context) => {return {};}
	});

	return dummyMap;
}

export function getDummyEsResult(messages: Linter.LintMessage[] = [getDummyEsMessage()]): ESLint.LintResult{
	return {
		filePath: "filePath",
		messages: messages,
		suppressedMessages: [],
		errorCount: 1,
		fatalErrorCount: 1,
		warningCount: 0,
		fixableErrorCount: 0,
		fixableWarningCount: 0,
		usedDeprecatedRules: []
	};
}

export function getDummyEsMessage(ruleId = 'rule', message = 'message'): Linter.LintMessage {
	return {
		fatal: true,
		ruleId: ruleId,
		severity: 2,
		line: 35,
		column: 7,
		message: message,
		fix: {
			range: [23, 78],
			text: "some fix string"
		}
	}
}

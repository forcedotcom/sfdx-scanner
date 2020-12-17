import { Rule, RuleGroup, RuleTarget, ESRule, ESResult, ESMessage, ESReport } from '../../../src/types';

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
		rulesets: [],
		defaultEnabled: true
	}
}

export function getDummyTarget(isDir: boolean = true): RuleTarget {
	return {
		target: "/some/target",
		isDirectory: isDir,
		paths: ['/some/target/path1', '/some/target/path2']
	}
}

export function getDummyEsRuleMap(ruleId: string = 'ruleId', category: string = 'myCategory', description: string = 'my description'): Map<string, ESRule> {
	const map = new Map<string, ESRule>();
	map.set(ruleId, getDummyEsRule(category, description));
	return map;
}

export function getDummyEsRule(category: string = 'myCategory', description: string = 'my description'): ESRule {
	return {
		meta: {
			docs: {
				description: description,
				category: category,
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	}
}

export function getDummyTypescriptRuleMap(): Map<string,ESRule> {
	const dummyMap: Map<string, ESRule> = new Map();
	dummyMap.set('fake-active', {
		meta: {
			docs: {
				description: 'Lorem Ipsum',
				category: 'Praise the Sun',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	});
	dummyMap.set('fake-deprecated', {
		meta: {
			deprecated: true,
			docs: {
				description: 'Lorem Ipsum',
				category: 'Praise the Sun',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	});
	dummyMap.set('fake-extended-a', {
		meta: {
			docs: {
				description: 'Lorem Ipsum',
				category: 'Praise the Sun',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	});
	dummyMap.set('fake-extended-b', {
		meta: {
			docs: {
				description: 'Lorem Ipsum',
				category: 'Praise the Sun',
				recommended: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	});
	dummyMap.set('@typescript-eslint/fake-extended-a', {
		meta: {
			docs: {
				description: 'Lorem Ipsum',
				category: 'Praise the Sun',
				recommended: true,
				extendsBaseRule: true,
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	});
	dummyMap.set('@typescript-eslint/renamed-b-extension', {
		meta: {
			docs: {
				description: 'Lorem Ipsum',
				category: 'Praise the Sun',
				recommended: true,
				extendsBaseRule: 'fake-extended-b',
				url: 'someURL'
			},
			schema: [{
				element: 'value'
			}]
		},
		create: () => { }
	});

	return dummyMap;
}

export function getDummyEsReport(results: ESResult[] = [getDummyEsResult()]): ESReport {
	return {
		results: results,
		errorCount: 0,
		warningCount: 0,
		fixableErrorCount: 0,
		fixableWarningCount: 0,
		usedDeprecatedRules: []
	}
}

export function getDummyEsResult(messages: ESMessage[] = [getDummyEsMessage()]): ESResult {
	return {
		filePath: "filePath",
		messages: messages
	};
}

export function getDummyEsMessage(ruleId: string = 'rule', message: string = 'message'): ESMessage {
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

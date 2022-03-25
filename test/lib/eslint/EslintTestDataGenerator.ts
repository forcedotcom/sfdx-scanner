import { Rule, RuleGroup, RuleTarget, ESRule, ESResult, ESMessage } from '../../../src/types';

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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
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
		/* eslint-disable @typescript-eslint/no-empty-function */
		create: () => { }
	});

	return dummyMap;
}

export function getDummyEsResult(messages: ESMessage[] = [getDummyEsMessage()]): ESResult {
	return {
		filePath: "filePath",
		messages: messages
	};
}

export function getDummyEsMessage(ruleId = 'rule', message = 'message'): ESMessage {
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

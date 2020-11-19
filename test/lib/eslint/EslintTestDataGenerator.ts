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
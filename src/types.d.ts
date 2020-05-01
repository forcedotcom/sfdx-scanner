export type Rule = {
	engine: string;
	sourcepackage: string;
	name: string;
	description: string;
	categories: string[];
	rulesets: string[];
	languages: string[];
	defaultEnabled: boolean;
	url?: string;
}

export type RuleGroup = {
	engine: string;
	name: string;
	paths: string[];
}

export type RuleResult = {
	engine: string;
	fileName: string;
	violations: RuleViolation[];
};

export type RuleViolation = {
	line: number;
	column: number;
	endLine?: number;
	endColumn?: number;
	ruleName: string;
	severity: number;
	message: string;
	category: string;
	url?: string;
};

export type Catalog = {
	rules: Rule[];
	categories: RuleGroup[];
	rulesets: RuleGroup[];
};

export type RuleEvent = {
	messageKey: string;
	args: string[];
	type: string;
	handler: string;
	verbose: boolean;
	time: number;
	internalLog?: string;
}

export type Rule = {
	engine: string;
	sourcepackage: string;
	name: string;
	description: string;
	categories: string[];
	rulesets: string[];
	languages: string[];
	defaultEnabled: boolean;
	defaultConfig?: any;
	url?: string;
}

export type LooseObject = {
	/* eslint-disable @typescript-eslint/no-explicit-any */
	[key: string]: any;
}

export type RuleGroup = {
	engine: string;
	name: string;
	paths: string[];
}

export type RuleTarget = {
	target: string;
	isDirectory?: boolean;
	paths: string[];
}
export type RuleResult = {
	engine: string;
	fileName: string;
	violations: RuleViolation[];
};

export type EngineExecutionSummary = {
	fileCount: number;
	violationCount: number;
};

export type RecombinedRuleResults = {
	minSev: number;
	results: string | {columns; rows};
	summaryMap: Map<string,EngineExecutionSummary>;
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

/**
 * Type mapping to rules returned from eslint
 */
export type ESRule = {
	meta: {
		deprecated?: boolean;
		docs: {
			description: string;
			category: string;
			recommended: boolean;
			extendsBaseRule?: boolean|string;
			url: string;
		};
		/* eslint-disable @typescript-eslint/no-explicit-any */
		schema: Record<string, any>[];
	};
	create: Function;
}

/**
 * Type mapping to report output by eslint
 */
export type ESReport = {
	results: ESResult[];
	errorCount: number;
	warningCount: number;
	fixableErrorCount: number;
	fixableWarningCount: number;
	usedDeprecatedRules: string[];
}

/**
 * Type mapping for eslint's results within a report
 */
export type ESResult = {
	filePath: string;
	messages: ESMessage[];
}

/**
 * Type mapping to report messages output by eslint
 */
export type ESMessage = {
	fatal: boolean;
	ruleId: string;
	severity: number;
	line: number;
	column: number;
	message: string;
	fix: {
		range: [number, number];
		text: string;
	};
}

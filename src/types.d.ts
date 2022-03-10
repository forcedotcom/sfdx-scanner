export type Rule = {
	engine: string;
	sourcepackage: string;
	name: string;
	description: string;
	categories: string[];
	rulesets: string[];
	languages: string[];
	defaultEnabled: boolean;
	// The expectation is that default configurations for other engines will be defined as their own types, which will
	// be OR'd together in this property.
	defaultConfig?: ESRuleConfig;
	url?: string;
}

export type LooseObject = {
	/* eslint-disable @typescript-eslint/no-explicit-any */
	[key: string]: any;
}

// This is a DESCRIPTIVE definition based on observation of ESLint and a few of its derivatives, NOT a proscriptive one
// to which all other ESLint-like engines are expected to adhere. If rules are discovered that violate this definition,
// changing the definition should be considered a viable option.
export type ESRuleConfig = string|Array<string|LooseObject>;

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

/**
 * A descriptor provided to a rule engine's `runEngine()` method, providing all information needed to run the engine.
 * @property ruleGroups - Groups of rules that should be run
 * @property rules - Individual rules that should be run
 * @property target - An array of targets for the rules to run against
 * @property engineOptions - A mapping of string keys to string values. Not all key-value pairs will apply to all engines.
 * @property normalizeSeverity - If true, the severity will be normalized to our global standard instead of engine-specific
 */
export type EngineExecutionDescriptor = {
	ruleGroups: RuleGroup[];
	rules: Rule[];
	target: RuleTarget[];
	engineOptions: Map<string,string>;
	normalizeSeverity: boolean;
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
	normalizedSeverity?: number;
	message: string;
	category: string;
	url?: string;
	exception?: boolean;
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

export type ESRuleMetadata = {
	deprecated?: boolean;
	// This is optional because ESLint-LWC rules don't have a type parameter set. We'll be defaulting to 'problem' in that case.
	type?: string;
	docs: {
		description: string;
		recommended: boolean|string;
		extendsBaseRule?: boolean|string;
		url: string;
	};
	/* eslint-disable @typescript-eslint/no-explicit-any */
	schema: Record<string, any>[];
}

/**
 * Type mapping to rules returned from eslint
 */
export type ESRule = {
	meta: ESRuleMetadata;
	create: Function;
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

export type TargetPattern = BasicTargetPattern|AdvancedTargetPattern;

export type AdvancedTargetPattern = {
	basePatterns: BasicTargetPattern[];
	advancedMatcher: TargetMatchingFunction;
}

export type TargetMatchingFunction = (t: string) => Promise<boolean>;

export type BasicTargetPattern = string;

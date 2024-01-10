import {JSONSchema4} from 'json-schema';
import {Linter} from 'eslint';
import {Ux} from '@salesforce/sf-plugins-core';
import {TargetType} from './Constants';

export type Rule = {
	engine: string;
	sourcepackage: string;
	name: string;
	description: string;
	categories: string[];
	rulesets: string[];
	languages: string[];
	defaultEnabled: boolean;
	isDfa: boolean;
	isPilot: boolean;
	// The expectation is that default configurations for other engines will be defined as their own types, which will
	// be OR'd together in this property.
	defaultConfig?: ESRuleConfigValue;
	url?: string;
}

export type TelemetryData = {
	eventName: string;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	[key: string]: any;
}

export type LooseObject = {
	/* eslint-disable-next-line @typescript-eslint/no-explicit-any */
	[key: string]: any;
}

// TODO: We should really define our input type property for each command so that we can avoid all the castings: "as string", "as boolean", etc
export type Inputs = {
	/* eslint-disable-next-line @typescript-eslint/no-explicit-any */
	[key: string]: any;
}

/**
 * Alias for ESLint's{@link Linter.RuleEntry} type. Exported here to minimize the need to import directly from ESLint.
 */
export type ESRuleConfigValue = Linter.RuleEntry;

/**
 * ESLint-like config files (e.g, ESLint's `recommended.js` file) will typically either be of this format, or contain
 * an object of this format.
 */
export type ESRuleConfig = {
	rules: Linter.RulesRecord
};

export type RuleGroup = {
	engine: string;
	name: string;
	paths: string[];
}

export type RuleTarget = {
	target: string;
	targetType: TargetType;
	paths: string[];
	methods?: string[];
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

export type ResultTableRow = {
	description: string;
	category: string;
	url: string;
	/**
	 * {@code location} applies solely to non-DFA violations, which are considered to occur
	 * at only a single point.
	 */
	location?: string;
	/**
	 * {@code sourceLocation} applies solely to DFA violations.
	 */
	sourceLocation?: string;
	/**
	 * {@code sinkLocation} applies solely to DFA violations.
	 */
	sinkLocation?: string;
}

// TODO: This is a bit of a smell. We should just return a string and treat all formatted output the same if possible.
export type FormattedOutput = string | {columns: Ux.Table.Columns<ResultTableRow>; rows: ResultTableRow[]};

type BaseViolation = {
	ruleName: string;
	message: string;
	severity: number;
	normalizedSeverity?: number;
	category: string;
	url?: string;
	exception?: boolean;
}

export type PathlessRuleViolation = BaseViolation & {
	line: number;
	column: number;
	endLine?: number;
	endColumn?: number;
};

export type DfaRuleViolation = BaseViolation & {
	sourceLine: number;
	sourceColumn: number;
	sourceType: string;
	sourceMethodName: string;
	sinkLine: number|null;
	sinkColumn: number|null;
	sinkFileName: string|null;
};

export type RuleViolation = PathlessRuleViolation | DfaRuleViolation;

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
 * All ESLint-like rules are defined such that ESLint can run them, but the engines have _exactly_ enough difference in
 * their type definitions that the TS compiler complains if you try to cast one to another. So we use this and {@link ESRule}
 * as super-types that we can use for our purposes, without worrying about a rule's true engine-specific type.
 */
export type ESRuleMetadata = {
	deprecated?: boolean;
	// This is optional because ESLint-LWC rules don't have a type parameter set. We'll be defaulting to 'problem' in that case.
	type?: string;
	docs?: {
		description?: string;
		recommended?: boolean|string;
		extendsBaseRule?: boolean|string;
		url?: string;
	};
	schema?: JSONSchema4|JSONSchema4[];
}

/**
 * All ESLint-like rules are defined such that ESLint can run them, but the engines have _exactly_ enough difference in
 * their type definitions that the TS compiler complains if you try to cast one to another. So we use this and {@link ESRuleMetadata}
 * as super-types that we can use for our purposes, without worrying about a rule's true engine-specific type.
 */
export type ESRule = {
	meta?: ESRuleMetadata;
	create: (LooseObject) => LooseObject;
}

export type TargetPattern = BasicTargetPattern|AdvancedTargetPattern;

export type AdvancedTargetPattern = {
	basePatterns: BasicTargetPattern[];
	advancedMatcher: TargetMatchingFunction;
}

export type TargetMatchingFunction = (t: string) => Promise<boolean>;

export type BasicTargetPattern = string;

export type SfgeConfig = {
	projectDirs: string[];
	ruleThreadCount?: number;
	ruleThreadTimeout?: number;
	ruleDisableWarningViolation?: boolean;
	jvmArgs?: string;
	pathexplimit?: number;
};

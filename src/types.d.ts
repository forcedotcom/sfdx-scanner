export type Rule = {
	engine: string;
	sourcepackage: string;
	name: string;
	description: string;
	categories: string[];
	rulesets: string[];
	languages: string[];
}

export type NamedPaths = {
	engine: string;
	name: string;
	paths: string[];
}

export type Catalog = {
	rules: Rule[];
	categories: NamedPaths[];
	rulesets: NamedPaths[];
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

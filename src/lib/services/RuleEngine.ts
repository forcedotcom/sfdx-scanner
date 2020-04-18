import {Rule, Catalog, NamedPaths} from '../../types';

export interface RuleEngine {
	getName(): string;

	// TODO deprecate this.  Engines don't provide rules directly.  They provide catalogs.
	getAll(): Promise<Rule[]>;

	getCatalog(): Promise<Catalog>;

	run(paths: NamedPaths[], target: string[] | string): Promise<string>;

	init(): Promise<void>;

	matchPath(path: string): boolean;
}

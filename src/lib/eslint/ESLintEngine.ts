import {Logger, SfdxError} from '@salesforce/core';
import {Catalog, NamedPaths, Rule} from '../../types';
import {RuleEngine} from '../services/RuleEngine';

export class ESLintEngine implements RuleEngine {
	public static NAME = "eslint";

	private logger: Logger;
	private initialized: boolean;

	public getName(): string {
		return ESLintEngine.NAME;
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	public async init(): Promise<void> {
		if (this.initialized) return;

		this.logger = await Logger.child(this.getName());

		this.initialized = true;
	}

	getCatalog(): Promise<Catalog> {
		return Promise.resolve({rulesets: [], categories: [], rules: []});
	}

	public async getAll(): Promise<Rule[]> {
		// PmdCatalogWrapper is a layer of abstraction between the commands and PMD, facilitating code reuse and other goodness.
		this.logger.trace('Getting PMD rules.');
		return Promise.resolve([]);
	}

	public async run(paths: NamedPaths[], target: string[]): Promise<string> {
		this.logger.trace(`About to run eslint rules. Target count: ${target.length}, filter count: ${paths.length}`);
		try {
			/*
			const linter = new Linter();

			const sample =
				{
					fatal: false,
					ruleId: "semi",
					severity: 2,
					line: 1,
					column: 23,
					message: "Expected a semicolon.",
					fix: {
						range: [1, 15],
						text: ";"
					}
				};

			const messages = linter.verify("var foo;", {
				rules: {
					semi: 2
				}
			}, {filename: "foo.js"});
*/

			const paths: string[] = [];
			// If we didn't find any paths, we're done.
			if (paths == null || paths.length === 0) {
				this.logger.trace('No Rule paths found. Nothing to execute.');
				return '';
			}
			const [violationsFound, stdout] = [null, null];

			if (violationsFound) {
				this.logger.trace('Found rule violations.');
				// If we found any violations, they'll be in an XML document somewhere in stdout, which we'll need to find and process.
				const xmlStart = stdout.indexOf('<?xml');
				const xmlEnd = stdout.lastIndexOf('</pmd>') + 6;
				const ruleViolationsXml = stdout.slice(xmlStart, xmlEnd);

				this.logger.trace(`Rule violations in the original XML format: ${ruleViolationsXml}`);
				return ruleViolationsXml;
			} else {
				// If we didn't find any violations, we can just return an empty string.
				this.logger.trace('No rule violations found.');
				return '';
			}
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

}

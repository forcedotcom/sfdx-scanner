import {Logger, SfdxError} from '@salesforce/core';
import {Rule, Catalog, PathGroup} from '../../types';
import {RuleEngine} from '../services/RuleEngine';
import {PmdCatalogWrapper} from './PmdCatalogWrapper';
import PmdWrapper from './PmdWrapper';

export class PmdEngine implements RuleEngine {
	public static NAME = "pmd";

	private logger: Logger;
	private pmdCatalogWrapper: PmdCatalogWrapper;
	private initialized: boolean;

	public getName(): string {
		return PmdEngine.NAME;
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}

		this.pmdCatalogWrapper = await PmdCatalogWrapper.create({});
		this.logger = await Logger.child(this.getName());

		this.initialized = true;
	}

	getCatalog(): Promise<Catalog> {
		return this.pmdCatalogWrapper.getCatalog();
	}

	public async getAll(): Promise<Rule[]> {
		// PmdCatalogWrapper is a layer of abstraction between the commands and PMD, facilitating code reuse and other goodness.
		this.logger.trace('Getting PMD rules.');
		const catalog = await this.pmdCatalogWrapper.getCatalog();
		return catalog.rules;
	}

	public async run(paths: PathGroup[], target: string[]): Promise<string> {
		this.logger.trace(`About to run PMD rules. Targets: ${target.length}, named paths: ${paths.length}`);
		try {
			// TODO: Weird translation to next layer. target=path and path=rule path. Consider renaming
			const [violationsFound, stdout] = await PmdWrapper.execute(target.join(','), paths.map(np => np.paths).join(','));
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

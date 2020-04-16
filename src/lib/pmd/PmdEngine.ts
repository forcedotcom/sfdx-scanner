import {Logger, SfdxError} from '@salesforce/core';
import {injectable} from 'inversify';
import {Rule} from '../../types';
import {RuleFilter} from '../RuleManager';
import {RuleEngine} from '../services/RuleEngine';
import {PmdCatalogWrapper} from './PmdCatalogWrapper';
import PmdWrapper from './PmdWrapper';

@injectable()
export class PmdEngine implements RuleEngine {
	public static NAME = "pmd";

	private logger: Logger;
	private pmdCatalogWrapper: PmdCatalogWrapper;

	public getName(): string {
		return PmdEngine.NAME;
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	public async init(): Promise<RuleEngine> {
		this.pmdCatalogWrapper = await PmdCatalogWrapper.create({});
		this.logger = await Logger.child('PmdEngine');
		return this;
	}

	public async getAll(): Promise<Rule[]> {
		// PmdCatalogWrapper is a layer of abstraction between the commands and PMD, facilitating code reuse and other goodness.
		this.logger.trace('Getting PMD rules.');
		const catalog = await this.pmdCatalogWrapper.getCatalog();
		return catalog.rules;
	}

	public async run(filters: RuleFilter[], target: string[]): Promise<string> {
		this.logger.trace(`About to run PMD rules. Target count: ${target.length}, filter count: ${filters.length}`);
		try {
			// Convert our filters into paths that we can feed back into PMD.
			const paths: string[] = await this.pmdCatalogWrapper.getPathsMatchingFilters(filters);
			// If we didn't find any paths, we're done.
			if (paths == null || paths.length === 0) {
				this.logger.trace('No Rule paths found. Nothing to execute.');
				return '';
			}
			// Otherwise, run PMD and see what we get.
			// TODO: Weird translation to next layer. target=path and path=rule path. Consider renaming
			const [violationsFound, stdout] = await PmdWrapper.execute(target.join(','), paths.join(','));

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

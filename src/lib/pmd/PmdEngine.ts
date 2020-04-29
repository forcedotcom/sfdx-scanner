import {Logger, SfdxError} from '@salesforce/core';
import {Element, xml2js} from 'xml-js';
import {Controller} from '../../ioc.config';
import {Catalog, Rule, RuleGroup, RuleResult} from '../../types';
import {RuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {PmdCatalogWrapper} from './PmdCatalogWrapper';
import PmdWrapper from './PmdWrapper';

interface PmdViolation extends Element {
	attributes: {
		begincolumn: number;
		beginline: number;
		endcolumn: number;
		endline: number;
		externalInfoUrl: string;
		priority: string;
		rule: string;
		ruleset: string;
	};
}

export class PmdEngine implements RuleEngine {
	public static NAME = "pmd";

	private logger: Logger;
	private config: Config;

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
		this.logger = await Logger.child(this.getName());

		this.config = await Controller.getConfig();
		this.pmdCatalogWrapper = await PmdCatalogWrapper.create({});

		this.initialized = true;
	}

	public isEnabled(): boolean {
		return this.config.isEngineEnabled(this.getName());
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

	/**
	 * Note: PMD is a little strange, only accepting rulesets or categories (aka Rule Groups) as input, rather than
	 * a list of rules.  Ideally we could pass in rules, like with other engines, filtered ahead of time by
	 * the catalog.  If that ever happens, we can remove the ruleGroups argument and use the rules directly.
	 */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: string[]): Promise<RuleResult[]> {
		this.logger.trace(`About to run PMD rules. Targets: ${targets.length}, rule groups: ${ruleGroups.length}`);
		if (ruleGroups.length === 0) {
			this.logger.trace(`No rule groups given.  PMD requires at least one. Skipping.`);
			return [];
		}
		try {
			// TODO: Weird translation to next layer. target=path and path=rule path. Consider renaming
			const [violationsFound, stdout] = await PmdWrapper.execute(targets.join(','), ruleGroups.map(np => np.paths).join(','));
			if (violationsFound) {
				this.logger.trace('Found rule violations.');
				// If we found any violations, they'll be in an XML document somewhere in stdout, which we'll need to find and process.

				const xmlStart = stdout.indexOf('<?xml');
				const xmlEnd = stdout.lastIndexOf('</pmd>') + 6;
				return this.xmlToRuleResults(stdout.slice(xmlStart, xmlEnd));
			} else {
				return [];
			}
		} catch (e) {
			throw new SfdxError(e.message || e);
		}
	}

	private xmlToRuleResults(pmdXml: string): RuleResult[] {
		// If the results were just an empty string, we can return it.
		if (pmdXml === '') {
			return [];
		}

		const pmdJson = xml2js(pmdXml, {compact: false, ignoreDeclaration: true});
		return pmdJson.elements[0].elements.map(
			(f): RuleResult => {
				return {
					engine: PmdEngine.NAME,
					fileName: f.attributes['name'],
					violations: f.elements.map(
						(v: PmdViolation) => {
							return {
								line: v.attributes.beginline,
								column: v.attributes.begincolumn,
								endLine: v.attributes.endline,
								endColumn: v.attributes.endcolumn,
								severity: v.attributes.priority,
								ruleName: v.attributes.rule,
								category: v.attributes.ruleset,
								url: v.attributes.externalInfoUrl,
								message: this.toText(v)
							};
						}
					)
				};
			}
		);
	}

	private toText(v: PmdViolation): string {
		if (v.elements.length === 0) {
			return '';
		}

		return v.elements.map(e => e.text).join("\n");
	}
}

import {Logger, SfdxError} from '@salesforce/core';
import * as path from 'path';
import {Element, xml2js} from 'xml-js';
import {Controller} from '../../ioc.config';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
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

	getTargetPatterns(path?: string): Promise<string[]> {
		const engineConfig = this.config.getEngineConfig(PmdEngine.NAME);
		return Promise.resolve(engineConfig.targetPatterns);
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

	/**
	 * Note: PMD is a little strange, only accepting rulesets or categories (aka Rule Groups) as input, rather than
	 * a list of rules.  Ideally we could pass in rules, like with other engines, filtered ahead of time by
	 * the catalog.  If that ever happens, we can remove the ruleGroups argument and use the rules directly.
	 */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[]): Promise<RuleResult[]> {
		if (ruleGroups.length === 0) {
			this.logger.trace(`No rule groups given.  PMD requires at least one. Skipping.`);
			return [];
		}
		try {
			const targetPaths: string[] = [];
			for (const target of targets) {
				if (target.isDirectory) {
					targetPaths.push(...target.paths.map(p => path.join(target.target, p)));
				} else {
					targetPaths.push(...target.paths);
				}
			}
			if (targetPaths.length === 0) {
				this.logger.trace('No matching pmd target files found. Nothing to execute.');
				return [];
			}
			this.logger.trace(`About to run PMD rules. Targets: ${targetPaths.length}, rule groups: ${ruleGroups.length}`);
			const [violationsFound, stdout] = await PmdWrapper.execute(targetPaths.join(','), ruleGroups.map(np => np.paths).join(','));
			if (violationsFound) {
				this.logger.trace('Found rule violations.');

				// Violations are in an XML document somewhere in stdout, which we'll need to find and process.
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

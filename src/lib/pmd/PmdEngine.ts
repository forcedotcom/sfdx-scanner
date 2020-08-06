import {Logger, SfdxError} from '@salesforce/core';
import {Element, xml2js} from 'xml-js';
import {Controller} from '../../ioc.config';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {RuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {ENGINE} from '../../Constants';
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
	public static NAME: string = ENGINE.PMD.valueOf();

	private logger: Logger;
	private config: Config;

	private pmdCatalogWrapper: PmdCatalogWrapper;
	private initialized: boolean;

	public getName(): string {
		return PmdEngine.NAME;
	}

	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	getTargetPatterns(path?: string): Promise<string[]> {
		return this.config.getTargetPatterns(ENGINE.PMD);
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
	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		if (ruleGroups.length === 0) {
			this.logger.trace(`No rule groups given.  PMD requires at least one. Skipping.`);
			return [];
		}
		try {
			const targetPaths: string[] = [];
			for (const target of targets) {
				targetPaths.push(...target.paths);
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
			this.logger.trace('Pmd evaluation failed: ' + (e.message || e));
			throw new SfdxError(e.message || e);
		}
	}

	protected xmlToRuleResults(pmdXml: string): RuleResult[] {
		// If the results were just an empty string, we can return it.
		if (pmdXml === '') {
			this.logger.trace('No PMD results to convert');
			return [];
		}

		const pmdJson = xml2js(pmdXml, {compact: false, ignoreDeclaration: true});

		// Only provide results for nodes that are files. Other nodes are filtered out and logged.
		const fileNodes = pmdJson.elements[0].elements.filter(e => 'file' === e.name);
		const otherNodes = pmdJson.elements[0].elements.filter(e => 'file' !== e.name);
		for (const otherNode of otherNodes) {
			this.logger.trace(`Skipping non-file node ${JSON.stringify(otherNode)}`);
		}

		return fileNodes.map(
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

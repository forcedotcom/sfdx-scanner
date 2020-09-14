import {Logger, Messages, SfdxError} from '@salesforce/core';
import {Element, xml2js} from 'xml-js';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget} from '../../types';
import {RuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {ENGINE} from '../../Constants';
import {PmdCatalogWrapper} from './PmdCatalogWrapper';
import PmdWrapper from './PmdWrapper';
import {uxEvents} from "../ScannerEvents";

Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages("@salesforce/sfdx-scanner", "EventKeyTemplates");

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
	private static THIS_ENGINE = ENGINE.PMD;
	public static ENGINE_NAME = PmdEngine.THIS_ENGINE.valueOf();

	private logger: Logger;
	private config: Config;

	private pmdCatalogWrapper: PmdCatalogWrapper;
	private initialized: boolean;

	public getName(): string {
		return PmdEngine.ENGINE_NAME;
	}

	getTargetPatterns(): Promise<string[]> {
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
		return this.config.isEngineEnabled(PmdEngine.THIS_ENGINE);
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
			const stdout = await PmdWrapper.execute(targetPaths.join(','), ruleGroups.map(np => np.paths).join(','));
			return this.processStdOut(stdout);
		} catch (e) {
			this.logger.trace('Pmd evaluation failed: ' + (e.message || e));
			throw new SfdxError(e.message || e);
		}
	}

	/**
	 * stdout returned from PMD contains an XML payload that may be surrounded by other text.
	 * 'file' nodes from the XML are returned as rows of output to the user. 'configerror', 'error',
	 * and 'suppressedviolation' nodes are emitted as warnings from the CLI.
	 */
	protected processStdOut(stdout: string): RuleResult[] {
		let violations: RuleResult[] = [];

		// Try to find the xml payload. It begins with '<?xml' and ends with '</pmd>'
		const pmdEnd = '</pmd>';
		const xmlStart = stdout.indexOf('<?xml');
		const xmlEnd = stdout.lastIndexOf(pmdEnd);
		if (xmlStart != -1 && xmlEnd != -1) {
			const pmdXml = stdout.slice(xmlStart, xmlEnd + pmdEnd.length);
			const pmdJson = xml2js(pmdXml, {compact: false, ignoreDeclaration: true});

			const elements =  pmdJson.elements[0].elements;
			if (elements) {
				this.emitErrorsAndWarnings(elements);
				violations = this.xmlToRuleResults(elements);
			}
		}

		if (violations.length > 0) {
			this.logger.trace('Found rule violations.');
		}

		return violations;
	}

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	protected xmlToRuleResults(elements: any): RuleResult[] {
		// Provide results for nodes that are files.
		const files = elements.filter(e => 'file' === e.name);

		return files.map(
			(f): RuleResult => {
				return {
					engine: PmdEngine.ENGINE_NAME,
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

	/**
	 * The PMD report contains a mix of violations and other issues. This method converts
	 * these other issues intoto ux events.
	 */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private emitErrorsAndWarnings(elements: any): void {
		// Provide results for nodes that aren't files.
		const nodes = elements.filter(e => 'file' !== e.name);

		// See https://github.com/pmd/pmd/blob/master/pmd-core/src/main/resources/report_2_0_0.xsd
		// for node schema
		for (const node of nodes) {
			const attributes = node.attributes;
			switch (node.name) {
				case "error":
					uxEvents.emit(
						"warning-always",
						messages.getMessage("warning.pmdSkippedFile", [
							attributes.filename,
							attributes.msg
						])
					);
					break;

				case "suppressedviolation":
					uxEvents.emit(
						"warning-always",
						messages.getMessage("warning.pmdSuppressedViolation", [
							attributes.filename,
							attributes.msg,
							attributes.suppressiontype,
							attributes.usermsg
						])
					);
					break;

				case "configerror":
					uxEvents.emit(
						"warning-always",
						messages.getMessage("warning.pmdConfigError", [
							attributes.rule,
							attributes.msg
						])
					);
					break;

				default:
					this.logger.warn(
						`Unknown non-file node ${JSON.stringify(node)}`
					);
			}
		}
	}

	private toText(v: PmdViolation): string {
		if (v.elements.length === 0) {
			return '';
		}

		return v.elements.map(e => e.text).join("\n");
	}
}

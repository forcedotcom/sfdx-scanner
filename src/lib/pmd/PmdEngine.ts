import {Logger, Messages, SfdxError} from '@salesforce/core';
import {Element, xml2js} from 'xml-js';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, TargetPattern} from '../../types';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {ENGINE, CUSTOM_CONFIG, EngineBase, HARDCODED_RULES} from '../../Constants';
import {PmdCatalogWrapper} from './PmdCatalogWrapper';
import PmdWrapper from './PmdWrapper';
import {uxEvents} from "../ScannerEvents";
import {FileHandler} from '../util/FileHandler';
import {EventCreator} from '../util/EventCreator';
import * as engineUtils from '../util/CommonEngineUtils';

Messages.importMessagesDirectory(__dirname);
const eventMessages = Messages.loadMessages("@salesforce/sfdx-scanner", "EventKeyTemplates");
const engineMessages = Messages.loadMessages("@salesforce/sfdx-scanner", "PmdEngine");

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

type HardcodedRuleDetail = {
	base: {
		name: string;
		category: string;
	};
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	nodeIdentifier: (node: any) => boolean;
	severity: number;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	msgFormatter: (node: any) => string;
};

const HARDCODED_RULE_DETAILS: HardcodedRuleDetail[] = [
	{
		base: HARDCODED_RULES.FILES_MUST_COMPILE,
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		nodeIdentifier: (node: any): boolean => {
			return node.name === 'error' && node.attributes.msg.toLowerCase().startsWith('pmdexception: error while parsing');
		},
		severity: 1,
		// eslint-disable-next-line @typescript-eslint/no-explicit-any
		msgFormatter: (node: any): string => {
			// We'll construct an array of strings and then join them together into a formatted message at the end.
			const msgComponents: string[] = [];

			// We want the message property that the node already has, since that's the one that directly described what happened.
			msgComponents.push(node.attributes.msg);
			// In general, the top-level exception will have exceptions below it that provide more information about lines,
			// columns, etc. Look for that.
			const causationStart = node.elements[0].cdata.indexOf('Caused by: ');
			if (causationStart > -1) {
				const causationEnd = node.elements[0].cdata.indexOf('\n', causationStart);
				msgComponents.push(node.elements[0].cdata.slice(causationStart, causationEnd));
			}
			// Now we'll combine our pieces and return that.
			return msgComponents.join('\n');
		}
	}
];


abstract class BasePmdEngine extends AbstractRuleEngine {

	protected logger: Logger;
	protected config: Config;

	protected pmdCatalogWrapper: PmdCatalogWrapper;
	protected eventCreator: EventCreator;
	private initialized: boolean;

	getTargetPatterns(): Promise<TargetPattern[]> {
		return this.config.getTargetPatterns(ENGINE.PMD);
	}

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	public abstract getName(): string;

	public abstract shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean;

	public abstract run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]>;

	public abstract isEnabled(): Promise<boolean>;

	public abstract isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean;

	public abstract getCatalog(): Promise<Catalog>;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());

		this.config = await Controller.getConfig();
		this.pmdCatalogWrapper = await PmdCatalogWrapper.create({});
		this.eventCreator = await EventCreator.create({});
		this.initialized = true;
	}

	protected async runInternal(selectedRules: string, targets: RuleTarget[]): Promise<RuleResult[]> {
		try {
			const targetPaths: string[] = [];
			for (const target of targets) {
				targetPaths.push(...target.paths);
			}
			if (targetPaths.length === 0) {
				this.logger.trace('No matching pmd target files found. Nothing to execute.');
				return [];
			}
			this.logger.trace(`About to run PMD rules. Targets: ${targetPaths.length}, Selected rules: ${selectedRules}`);

			const selectedTargets = targetPaths.join(',');
			const stdout = await PmdWrapper.execute(selectedTargets, selectedRules);
			const results = this.processStdOut(stdout);
			this.logger.trace(`Found ${results.length} for PMD`);
			return results;
		} catch (e) {
			this.logger.trace('Pmd evaluation failed: ' + (e.message || e));
			throw new SfdxError(this.processStdErr(e.message || e));
		}
	}


	/**
	 * stdout returned from PMD contains an XML payload that may be surrounded by other text.
	 * 'file' nodes from the XML are returned as rows of output to the user. 'configerror', 'error',
	 * and 'suppressedviolation' nodes are emitted as warnings from the CLI.
	 */
	protected processStdOut(stdout: string): RuleResult[] {
		let violations: RuleResult[] = [];

		this.logger.trace(`Output received from PMD: ${stdout}`);

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

	/**
	 * Identifies some common PMD errors and translates the messy error messages into something a bit more understandable.
	 * @param {string} stderr - The stderr stream from a PMD execution.
	 * @returns {string} The error, reinterpreted and simplified to the best of our abilities.
	 */
	private processStdErr(stderr: string): string {
		if (stderr.includes('SEVERE: Ruleset not found') && stderr.includes('RuleSetNotFoundException')) {
			return this.translateRuleSetNotFoundException(stderr);
		} else {
			// If the error didn't match any of the templates that we know we can convert, just return it as-is.
			return stderr;
		}
	}

	/**
	 * Converts the messy output of a PMD RuleSetNotFoundException into something more useful to the user.
	 * @param {string} stderr - The stderr stream from a PMD execution.
	 * @returns {string} The error, reinterpreted and simplified to the best of our abilities.
	 */
	private translateRuleSetNotFoundException(stderr: string): string {
		// Errors of this type will have a 'RulesetNotFoundException' present.
		const errRegex = /RuleSetNotFoundException: Can't find resource '(\S+)' for rule '(\S+)'/;
		const regexResult: string[] = errRegex.exec(stderr);
		// We're expecting the results to be a length-3 array, with the first item being the entire match, and the second
		// and third being the capture groups we defined in the regex.
		if (regexResult && regexResult.length === 3) {
			return engineMessages.getMessage('errorTemplates.rulesetNotFoundTemplate', regexResult.slice(1));
		} else {
			// If we weren't able to match the template, just return the log we were given. That way, at least we don't
			// make it any worse.
			return stderr;
		}
	}

	/**
	 * Indicates whether the scanner should consider a given node as representing PMD violations. Some nodes don't technically
	 * contain violations, but should be treated as fake violations of fake rules.
	 * @param node - A top-level node from PMD's results.
	 * @returns {boolean} true if the node represents rule violations in the scanner's opinion.
	 * @private
	 */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private nodeRepresentsViolations = (node: any): boolean => {
		// `file` nodes always contain violations.
		if (node.name === 'file') {
			return true;
		}
		// Other types of nodes don't technically contain violations, but sometimes they indicate problems that we want
		// to surface in the same manner as true violations, instead of errors or warnings.
		return HARDCODED_RULE_DETAILS.some((detail: HardcodedRuleDetail): boolean => detail.nodeIdentifier(node));
	};

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	protected xmlToRuleResults(elements: any): RuleResult[] {
		// Provide results for nodes that are files.
		const violationNodes = elements.filter(this.nodeRepresentsViolations);

		return violationNodes.map((node): RuleResult => {
			switch (node.name) {
				case 'file':
					return this.fileNodeToRuleResult(node);
				case 'error':
					return this.errorNodeToRuleResult(node);
				default:
					// This shouldn't be possible, but it's best practice for every switch to have a default. We'd respond
					// by logging a verbose warning about the node type to the user, and writing the actual node itself
					// to the internal logs.
					uxEvents.emit(
						"warning-verbose",
						eventMessages.getMessage('warning.unexpectedPmdNodeType', [
							node.name
						])
					);
					this.logger.warn(`Unknown result node ${JSON.stringify(node)}`);
					return null;
			}
		}).filter((rr: RuleResult): boolean => rr != null);
	}

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private fileNodeToRuleResult(element: any): RuleResult {
		return {
			engine: this.getName(),
			fileName: element.attributes['name'],
			violations: element.elements.map(
				(v: PmdViolation) => {
					return {
						line: v.attributes.beginline,
						column: v.attributes.begincolumn,
						endLine: v.attributes.endline,
						endColumn: v.attributes.endcolumn,
						severity: Number(v.attributes.priority),
						ruleName: v.attributes.rule,
						category: v.attributes.ruleset,
						url: v.attributes.externalInfoUrl,
						message: this.toText(v)
					};
				}
			)
		};
	}

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private errorNodeToRuleResult(element: any): RuleResult {
		const hardcodedDetails = HARDCODED_RULE_DETAILS.find(details => details.nodeIdentifier(element));

		return {
			engine: this.getName(),
			fileName: element.attributes.filename,
			violations: [{
				line: 1,
				column: 1,
				severity: hardcodedDetails.severity,
				ruleName: hardcodedDetails.base.name,
				category: hardcodedDetails.base.category,
				message: hardcodedDetails.msgFormatter(element),
				exception: true
			}]
		};
	}

	/**
	 * The PMD report contains a mix of violations and other issues. This method converts
	 * these other issues intoto ux events.
	 */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private emitErrorsAndWarnings(elements: any): void {
		// Provide results for nodes that aren't files.
		const nodes = elements.filter(e => !this.nodeRepresentsViolations(e));

		// See https://github.com/pmd/pmd/blob/master/pmd-core/src/main/resources/report_2_0_0.xsd
		// for node schema
		for (const node of nodes) {
			const attributes = node.attributes;
			switch (node.name) {
				case "error":
					uxEvents.emit(
						"warning-always",
						eventMessages.getMessage("warning.pmdSkippedFile", [
							attributes.filename,
							attributes.msg
						])
					);
					break;

				case "suppressedviolation":
					uxEvents.emit(
						"warning-always",
						eventMessages.getMessage("warning.pmdSuppressedViolation", [
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
						eventMessages.getMessage("warning.pmdConfigError", [
							attributes.rule,
							attributes.msg
						])
					);
					break;

				default:
					uxEvents.emit(
						"warning-verbose",
						eventMessages.getMessage('warning.unexpectedPmdNodeType', [
							node.name
						])
					);
					this.logger.warn(
						`Unknown non-result node ${JSON.stringify(node)}`
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

function isCustomRun(engineOptions: Map<string, string>): boolean {
	return engineUtils.isCustomRun(CUSTOM_CONFIG.PmdConfig, engineOptions);
}

export class PmdEngine extends BasePmdEngine {
	private static THIS_ENGINE = ENGINE.PMD;
	public static ENGINE_NAME = PmdEngine.THIS_ENGINE.valueOf();

	getName(): string {
		return PmdEngine.ENGINE_NAME;
	}

	getCatalog(): Promise<Catalog> {
		return this.pmdCatalogWrapper.getCatalog();
	}

	shouldEngineRun(
		ruleGroups: RuleGroup[],
		rules: Rule[],
		target: RuleTarget[],
		engineOptions: Map<string, string>): boolean {
		return !isCustomRun(engineOptions)
			&& (ruleGroups.length > 0); // TODO: there's a bug in DefaultRuleManager that's populating Rules instead of RuleGroups when --engine filter is used
		//TODO: targetPaths count should be ideally included here
	}

	isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean {
		return !isCustomRun(engineOptions)
		&& engineUtils.isFilterEmptyOrNameInFilter(this.getName(), filterValues);
	}

	/**
	 * Note: PMD is a little strange, only accepting rulesets or categories (aka Rule Groups) as input, rather than
	 * a list of rules.  Ideally we could pass in rules, like with other engines, filtered ahead of time by
	 * the catalog.  If that ever happens, we can remove the ruleGroups argument and use the rules directly.
	 */
	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {

		this.logger.trace(`${ruleGroups.length} Rules found for PMD engine`);

		const selectedRules = ruleGroups.map(np => np.paths).join(',');
		return await this.runInternal(selectedRules, targets);
	}

	public async isEnabled(): Promise<boolean> {
		return await this.config.isEngineEnabled(PmdEngine.THIS_ENGINE);
	}
}

export class CustomPmdEngine extends BasePmdEngine {
	private static THIS_ENGINE = ENGINE.PMD_CUSTOM;

	getName(): string {
		return CustomPmdEngine.THIS_ENGINE.valueOf();
	}

	isEnabled(): Promise<boolean> {
		return Promise.resolve(true); // Custom config will always be enabled
	}

	getCatalog(): Promise<Catalog> {
		// TODO: revisit this when adding customization to List
		const catalog = {
			rules: [],
			categories: [],
			rulesets: []
		};
		return Promise.resolve(catalog);
	}

	shouldEngineRun(
		ruleGroups: RuleGroup[],
		rules: Rule[],
		target: RuleTarget[],
		engineOptions: Map<string, string>): boolean {
		return isCustomRun(engineOptions);
		//TODO: targetPaths count should be ideally included here
	}

	isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean {
		return isCustomRun(engineOptions)
		&& engineUtils.isFilterEmptyOrFilterValueStartsWith(EngineBase.PMD, filterValues);
	}

	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	public async run(
		ruleGroups: RuleGroup[],
		rules: Rule[],
		targets: RuleTarget[],
		engineOptions: Map<string, string>): Promise<RuleResult[]> {

		const selectedRules = await this.getCustomConfig(engineOptions);

		// Let users know that they are on their own
		this.eventCreator.createUxInfoAlwaysMessage('info.customPmdHeadsUp', [selectedRules]);

		if (ruleGroups.length > 0) {
			this.eventCreator.createUxInfoAlwaysMessage('info.filtersIgnoredCustom', []);
		}

		return await this.runInternal(selectedRules, targets);
	}

	private async getCustomConfig(engineOptions: Map<string, string>): Promise<string> {
		const configFile = engineOptions.get(CUSTOM_CONFIG.PmdConfig);
		const fileHandler = new FileHandler();
		if (!(await fileHandler.exists(configFile))) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'PmdEngine', 'ConfigNotFound', [configFile]);
		}

		return configFile;
	}

}



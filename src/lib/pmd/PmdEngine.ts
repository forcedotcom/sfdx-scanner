import {Logger, SfError} from '@salesforce/core';
import {Element, xml2js} from 'xml-js';
import {Controller} from '../../Controller';
import {
	Catalog,
	Rule,
	RuleGroup,
	RuleResult,
	RuleTarget,
	RuleViolation,
	TargetPattern
} from '../../types';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {APPEXCHANGE_PMD_LIB, PMD_APPEXCHANGE_RULES_VERSION, CUSTOM_CONFIG, ENGINE, EngineBase, HARDCODED_RULES, Severity} from '../../Constants';
import {PmdCatalogWrapper} from './PmdCatalogWrapper';
import PmdWrapper from './PmdWrapper';
import {EVENTS, uxEvents} from "../ScannerEvents";
import {FileHandler} from '../util/FileHandler';
import {EventCreator} from '../util/EventCreator';
import * as engineUtils from '../util/CommonEngineUtils';
import {BundleName, getMessage} from "../../MessageCatalog";
import * as PrettyPrinter from '../util/PrettyPrinter';
import * as PmdLanguageManager from './PmdLanguageManager';
import path = require('path');
import {AsyncCreatable} from "@salesforce/kit";

interface PmdViolation extends Element {
	attributes: {
		begincolumn: string;
		beginline: string;
		endcolumn: string;
		endline: string;
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
	nodeIdentifier: (node: Element) => boolean;
	severity: number;
	msgFormatter: (node: Element) => string;
};

const HARDCODED_RULE_DETAILS: HardcodedRuleDetail[] = [
	{
		base: HARDCODED_RULES.FILES_MUST_COMPILE,
		nodeIdentifier: (node: Element): boolean => {
			return node.name === 'error' && (node.attributes.msg as string).toLowerCase().startsWith('pmdexception: error while parsing');
		},
		severity: 1,
		msgFormatter: (node: Element): string => {
			// We'll construct an array of strings and then join them together into a formatted message at the end.
			const msgComponents: string[] = [];

			// We want the message property that the node already has, since that's the one that directly described what happened.
			msgComponents.push(node.attributes.msg as string);
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


abstract class AbstractPmdEngine extends AbstractRuleEngine {
	/**
	 * As per our internal policies, we want to avoid significantly changing output in minor releases, except for
	 * high-severity security rules.
	 * This Map is a temporary solution allowing us to silence new rules until they can be properly integrated.
	 * @private
	 */
	private SKIPPED_RULES_TO_REASON_MAP: Map<string,string> = new Map([['apexunittestclassshouldhaverunas', 'Semantic version incompatible']]);

	protected logger: Logger;
	protected config: Config;

	protected eventCreator: EventCreator;
	private initialized: boolean;

	public matchPath(path: string): boolean {
		// TODO implement this for realz
		return path != null;
	}

	public abstract getName(): string;

	public abstract shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean;

	public abstract run(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]>;

	public abstract isEnabled(): Promise<boolean>;

	public abstract isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean;

	public isDfaEngine(): boolean {
		return false;
	}

	getNormalizedSeverity(severity: number): Severity {
		switch (severity) {
			case 1:
				return Severity.HIGH;
			case 2:
				return Severity.MODERATE;
			case 3:
			case 4:
			case 5:
				return Severity.LOW;
			default:
				return Severity.MODERATE;
		}
	}

	public abstract getCatalog(): Promise<Catalog>;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());

		this.config = await Controller.getConfig();
		this.eventCreator = await EventCreator.create({});
		this.initialized = true;
	}

	protected async runInternal(selectedRules: string, targets: RuleTarget[], rulePathsByLanguage: Map<string, Set<string>>, supplementalClasspath: string[] = []): Promise<RuleResult[]> {
		try {
			const targetPaths: string[] = [];
			for (const target of targets) {
				targetPaths.push(...target.paths);
			}
			if (targetPaths.length === 0) {
				this.logger.trace('No matching pmd target files found. Nothing to execute.');
				return [];
			}
			const fh = new FileHandler();
			const reportFile: string = await fh.tmpFileWithCleanup();
			this.logger.trace(`About to run PMD rules. Targets: ${targetPaths.length}, Selected rules: ${selectedRules}`);

			const stdout = await (await PmdWrapper.create({
				targets: targetPaths,
				rules: selectedRules,
				rulePathsByLanguage,
				supplementalClasspath,
				reportFile
			})).execute();
			this.logger.trace(`STDOUT from PMD: ${stdout}`);
			const rawResults: string = await fh.readFile(reportFile);
			const results = this.processOutput(rawResults);
			this.logger.trace(`Found ${results.length} for PMD`);
			return results;
		} catch (e) {
			const message: string = e instanceof Error ? e.message : e as string;
			this.logger.trace(`Pmd evaluation failed: ${message}`);
			throw new SfError(this.processStdErr(message));
		}
	}


	/**
	 * PMD output is represented as an XML payload.
	 * 'file' nodes from the XML are returned as rows of output to the user. 'configerror', 'error',
	 * and 'suppressedviolation' nodes are emitted as warnings from the CLI.
	 */
	protected processOutput(output: string): RuleResult[] {
		let results: RuleResult[] = [];

		this.logger.trace(`Output received from PMD: ${output}`);

		// Try to find the xml payload. It begins with '<?xml' and ends with '</pmd>'
		const pmdEnd = '</pmd>';
		const xmlStart = output.indexOf('<?xml');
		const xmlEnd = output.lastIndexOf(pmdEnd);
		if (xmlStart != -1 && xmlEnd != -1) {
			const pmdXml = output.slice(xmlStart, xmlEnd + pmdEnd.length);
			const pmdJson = xml2js(pmdXml, {compact: false, ignoreDeclaration: true}) as Element;

			const elements =  pmdJson.elements[0].elements;
			if (elements) {
				this.emitErrorsAndWarnings(elements);
				results = this.xmlToRuleResults(elements);
			}
		}

		// For now, we're handling filtering by running all rules, and then removing results for the skipped rules post-facto.
		// This is acceptable currently, since we're only skipping rules that are functional but incompatible with our
		// versioning schema.
		results = this.filterSkippedRulesFromResults(results);

		if (results.length > 0) {
			this.logger.trace('Found rule violations.');
		}

		return results;
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
			return getMessage(BundleName.PmdEngine, 'errorTemplates.rulesetNotFoundTemplate', regexResult.slice(1));
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
	private nodeRepresentsViolations = (node: Element): boolean => {
		// `file` nodes always contain violations.
		if (node.name === 'file') {
			return true;
		}
		// Other types of nodes don't technically contain violations, but sometimes they indicate problems that we want
		// to surface in the same manner as true violations, instead of errors or warnings.
		return HARDCODED_RULE_DETAILS.some((detail: HardcodedRuleDetail): boolean => detail.nodeIdentifier(node));
	};

	protected xmlToRuleResults(elements: Element[]): RuleResult[] {
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
						EVENTS.WARNING_VERBOSE,
						getMessage(BundleName.EventKeyTemplates, 'warning.unexpectedPmdNodeType', [
							node.name
						])
					);
					this.logger.warn(`Unknown result node ${JSON.stringify(node)}`);
					return null;
			}
		}).filter((rr: RuleResult): boolean => rr != null);
	}

	private fileNodeToRuleResult(element: Element): RuleResult {
		return {
			engine: this.getName(),
			fileName: element.attributes['name'] as string,
			violations: element.elements.map(
				(v: PmdViolation) => {
					return {
						line: Number(v.attributes.beginline),
						column: Number(v.attributes.begincolumn),
						endLine: Number(v.attributes.endline),
						endColumn: Number(v.attributes.endcolumn),
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

	private errorNodeToRuleResult(element: Element): RuleResult {
		const hardcodedDetails = HARDCODED_RULE_DETAILS.find(details => details.nodeIdentifier(element));

		return {
			engine: this.getName(),
			fileName: element.attributes.filename as string,
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

	private filterSkippedRulesFromResults(unfilteredResults: RuleResult[]): RuleResult[] {
		const filteredResults: RuleResult[] = [];
		const alreadySkippedRules: Set<string> = new Set();
		for (const result of unfilteredResults) {
			const filteredViolations: RuleViolation[] = [];
			for (const violation of result.violations) {
				const ruleKey = violation.ruleName.toLowerCase();
				// If the violation isn't for a skipped rule, add it to the filtered list.
				if (!this.SKIPPED_RULES_TO_REASON_MAP.has(ruleKey)) {
					filteredViolations.push(violation);
				} else if (!alreadySkippedRules.has(ruleKey)) {
					// If this is the first time we're skipping a violation for this particular rule, throw a verbose-only
					// info-log about it.
					uxEvents.emit(
						EVENTS.INFO_VERBOSE,
						getMessage(BundleName.EventKeyTemplates, "info.pmdRuleSkipped", [
							violation.ruleName, this.SKIPPED_RULES_TO_REASON_MAP.get(ruleKey)
						])
					);
					alreadySkippedRules.add(ruleKey);
				}
			}
			// If there are still any violations after filtering, then create a new result and add it to the filtered list.
			if (filteredViolations.length > 0) {
				filteredResults.push({
					engine: result.engine,
					fileName: result.fileName,
					violations: filteredViolations
				});
			}
		}
		return filteredResults;
	}

	/**
	 * The PMD report contains a mix of violations and other issues. This method converts
	 * these other issues intoto ux events.
	 */
	private emitErrorsAndWarnings(elements: Element[]): void {
		// Provide results for nodes that aren't files.
		const nodes = elements.filter(e => !this.nodeRepresentsViolations(e));

		// See https://github.com/pmd/pmd/blob/master/pmd-core/src/main/resources/report_2_0_0.xsd
		// for node schema
		for (const node of nodes) {
			const attributes = node.attributes;
			switch (node.name) {
				case "error":
					uxEvents.emit(
						EVENTS.WARNING_ALWAYS,
						getMessage(BundleName.EventKeyTemplates, "warning.pmdSkippedFile", [
							attributes.filename,
							attributes.msg
						])
					);
					break;

				case "suppressedviolation":
					uxEvents.emit(
						EVENTS.WARNING_ALWAYS,
						getMessage(BundleName.EventKeyTemplates, "warning.pmdSuppressedViolation", [
							attributes.filename,
							attributes.msg,
							attributes.suppressiontype,
							attributes.usermsg
						])
					);
					break;

				case "configerror":
					uxEvents.emit(
						EVENTS.WARNING_ALWAYS,
						getMessage(BundleName.EventKeyTemplates, "warning.pmdConfigError", [
							attributes.rule,
							attributes.msg
						])
					);
					break;

				default:
					uxEvents.emit(
						EVENTS.WARNING_VERBOSE,
						getMessage(BundleName.EventKeyTemplates, 'warning.unexpectedPmdNodeType', [
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

export class PmdEngine extends AbstractPmdEngine {
	private static THIS_ENGINE = ENGINE.PMD;
	public static ENGINE_NAME = PmdEngine.THIS_ENGINE.valueOf();
	private catalogWrapper: PmdCatalogWrapper;

	getName(): string {
		return PmdEngine.ENGINE_NAME;
	}

	getTargetPatterns(): Promise<TargetPattern[]> {
		return this.config.getTargetPatterns(ENGINE.PMD);
	}

	async getCatalog(): Promise<Catalog> {
		if (!this.catalogWrapper) {
			this.catalogWrapper = await PmdCatalogWrapper.create({
				rulePathsByLanguage: await (await _PmdRuleMapper.create({})).createStandardRuleMap(),
				catalogedEngineName: PmdEngine.ENGINE_NAME
			});
		}
		return this.catalogWrapper.getCatalog();
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
	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {

		this.logger.trace(`${ruleGroups.length} Rules found for PMD engine`);

		const selectedRules = ruleGroups.map(np => np.paths).join(',');
		return await this.runInternal(selectedRules, targets, await (await _PmdRuleMapper.create({})).createStandardRuleMap());
	}

	public isEnabled(): Promise<boolean> {
		return this.config.isEngineEnabled(PmdEngine.THIS_ENGINE);
	}
}

export class CustomPmdEngine extends AbstractPmdEngine {
	private static THIS_ENGINE = ENGINE.PMD_CUSTOM;

	getName(): string {
		return CustomPmdEngine.THIS_ENGINE.valueOf();
	}

	getTargetPatterns(): Promise<TargetPattern[]> {
		// The Custom variant shares the same target patterns as the standard variant.
		return this.config.getTargetPatterns(ENGINE.PMD);
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

	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	public async run(
		ruleGroups: RuleGroup[],
		rules: Rule[],
		targets: RuleTarget[],
		engineOptions: Map<string, string>): Promise<RuleResult[]> {

		const selectedRules = await this.getCustomConfig(engineOptions);

		// Let users know that they are on their own
		await this.eventCreator.createUxInfoAlwaysMessage('info.customPmdHeadsUp', [selectedRules]);

		if (ruleGroups.length > 0) {
			await this.eventCreator.createUxInfoAlwaysMessage('info.filtersIgnoredCustom', []);
		}

		return await this.runInternal(selectedRules, targets, await (await _PmdRuleMapper.create({})).createStandardRuleMap());
	}

	private async getCustomConfig(engineOptions: Map<string, string>): Promise<string> {
		const configFile = engineOptions.get(CUSTOM_CONFIG.PmdConfig);
		const fileHandler = new FileHandler();
		if (!(await fileHandler.exists(configFile))) {
			throw new SfError(getMessage(BundleName.PmdEngine, 'ConfigNotFound', [configFile]));
		}

		return configFile;
	}
}

export class AppExchangePmdEngine extends AbstractPmdEngine {
	private static readonly SUPPORTED_LANGUAGES = ['apex', 'html', 'javascript', 'sfmetadata', 'visualforce', 'xml'];
	private static ENGINE_ENUM = ENGINE.PMD_APPEXCHANGE;
	public static ENGINE_NAME = AppExchangePmdEngine.ENGINE_ENUM.valueOf();
	private catalogWrapper: PmdCatalogWrapper;

	getName(): string {
		return AppExchangePmdEngine.ENGINE_NAME;
	}

	getTargetPatterns(): Promise<TargetPattern[]> {
		return this.config.getTargetPatterns(ENGINE.PMD_APPEXCHANGE);
	}

	async getCatalog(): Promise<Catalog> {
		if (!this.catalogWrapper) {
			this.catalogWrapper = await PmdCatalogWrapper.create({
				rulePathsByLanguage: this.createRuleMap(),
				catalogedEngineName: AppExchangePmdEngine.ENGINE_NAME
			});
		}
		return this.catalogWrapper.getCatalog();
	}

	shouldEngineRun(
		ruleGroups: RuleGroup[],
		rules: Rule[],
		target: RuleTarget[],
		engineOptions: Map<string, string>): boolean {
		// If this isn't a custom run, and there are rule groups for the engine to run,
		// then we're good.
		return !isCustomRun(engineOptions) && (ruleGroups.length > 0);
	}

	isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean {
		// If the analyzer run isn't custom, then this engine counts as requested by default.
		return !isCustomRun(engineOptions) && engineUtils.isFilterEmptyOrNameInFilter(this.getName(), filterValues);
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		const selectedRules = ruleGroups.map(np => np.paths).join(',');
		return await this.runInternal(selectedRules, targets, this.createRuleMap(), [`${APPEXCHANGE_PMD_LIB}/*`]);
	}

	public isEnabled(): Promise<boolean> {
		return this.config.isEngineEnabled(AppExchangePmdEngine.ENGINE_ENUM);
	}

	private createRuleMap(): Map<string, Set<string>> {
		const rulePathsByLanguage = new Map<string, Set<string>>();
		for (const language of AppExchangePmdEngine.SUPPORTED_LANGUAGES) {
			const jarPath = path.join(`${APPEXCHANGE_PMD_LIB}`, `sfca-pmd-${language}-${PMD_APPEXCHANGE_RULES_VERSION}.jar`);
			rulePathsByLanguage.set(language, new Set<string>([jarPath]));
		}
		return rulePathsByLanguage;
	}
}

/**
 * Helper class for PMD variants that need to catalog PMD's default rules and any user-registered rules.
 */
export class _PmdRuleMapper extends AsyncCreatable {
	private initialized: boolean;
	private logger: Logger;

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}

		this.logger = await Logger.child(this.constructor.name);
	}

	/**
	 * Creates a mapping from language names to sets of files that define PMD rules for those languages.
	 * The mapped files encompass PMD's default rules for activated languages, as well as any user-registered
	 * custom rules.
	 */
	public async createStandardRuleMap(): Promise<Map<string, Set<string>>> {
		const rulePathsByLanguage = new Map<string, Set<string>>();
		// Add the default PMD jar for each activated language.
		(await PmdLanguageManager.getSupportedLanguages()).forEach(language => {
			const rulePaths = rulePathsByLanguage.get(language) || new Set<string>();

			const pmdJarPath = Controller.getActivePmdCommandInfo().getJarPathForLanguage(language);
			rulePaths.add(pmdJarPath);
			this.logger.trace(`Adding JAR ${pmdJarPath}, the default PMD JAR for language ${language}`);

			rulePathsByLanguage.set(language, rulePaths);
		});

		// Next, handle custom paths.
		const fileHandler = new FileHandler();
		const customRulePaths: Map<string, Set<string>> = (await Controller.createRulePathManager()).getRulePathEntries(PmdEngine.ENGINE_NAME);
		for (const [languageKey, rulePathSet] of customRulePaths.entries()) {
			// Attempt to de-alias the language key into one that PMD will recognize.
			// If that doesn't work, just cast the key to lowercase.
			const finalKey = (await PmdLanguageManager.resolveLanguageAlias(languageKey)) || languageKey.toLowerCase();
			this.logger.trace(`Custom path language key ${languageKey} converted to cataloger language key ${finalKey}`);

			// Next we want to do an existence check on each of the custom paths.
			const mappedPathSet = rulePathsByLanguage.get(finalKey) || new Set<string>();
			if (rulePathSet) {
				for (const rulePath of rulePathSet.values()) {
					if (await fileHandler.exists(rulePath)) {
						// If the path matches an existing file/directory, add it to the map
						// and the classpath.
						mappedPathSet.add(rulePath);
					} else {
						// If the path doesn't match an existing file, then the file may have been
						// deleted or moved. Warn the user about that.
						uxEvents.emit(EVENTS.WARNING_ALWAYS, getMessage(BundleName.EventKeyTemplates, 'warning.customRuleFileNotFound', [rulePath, finalKey]));
					}
				}
			}
			rulePathsByLanguage.set(finalKey, mappedPathSet);
		}
		this.logger.trace(`Found PMD rule paths ${PrettyPrinter.stringifyMapOfSets(rulePathsByLanguage)}`);
		return rulePathsByLanguage;
	}
}

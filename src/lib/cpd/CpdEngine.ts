import {Logger, Messages, SfError} from '@salesforce/core';
import {xml2js, Element} from 'xml-js';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, TargetPattern} from '../../types';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {ENGINE, LANGUAGE, Severity} from '../../Constants';
import * as engineUtils from '../util/CommonEngineUtils';
import CpdWrapper from './CpdWrapper';
import {uxEvents, EVENTS} from '../ScannerEvents';
import crypto = require('crypto');
import * as EnvVariable from '../util/EnvironmentVariable';


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'CpdEngine');
const eventMessages = Messages.loadMessages("@salesforce/sfdx-scanner", "EventKeyTemplates");

//  CPD supported languages: [apex, java, vf, xml]
const FileExtToLanguage: Map<string, LANGUAGE> = new Map([
	// apex
	['cls', LANGUAGE.APEX],
	['trigger', LANGUAGE.APEX],
	// java
	['java', LANGUAGE.JAVA],
	// vf
	['component', LANGUAGE.VISUALFORCE],
	['page', LANGUAGE.VISUALFORCE],
	// xml
	['xml', LANGUAGE.XML],
]);


// exported for visibility in tests
export const CpdRuleName = 'copy-paste-detected';
export const CpdRuleDescription = 'Identify duplicate code blocks.';
export const CpdRuleCategory = 'Copy/Paste Detected';
export const CpdInfoUrl = 'https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates';
export const CpdViolationSeverity = Severity.LOW;
export const CpdLanguagesSupported: LANGUAGE[] = [...new Set (FileExtToLanguage.values())];

export class CpdEngine extends AbstractRuleEngine {

	private readonly ENGINE_ENUM: ENGINE = ENGINE.CPD;
	private readonly ENGINE_NAME: string = ENGINE.CPD.valueOf();


	private minimumTokens: number;
	private logger: Logger;
	private config: Config;
	private initialized = false;

	public getName(): string {
		return this.ENGINE_NAME;
	}

	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.config = await Controller.getConfig();
		this.minimumTokens = EnvVariable.getEnvVariableAsNumber(this.ENGINE_ENUM, EnvVariable.CONFIG_NAME.MINIMUM_TOKENS) || ( await this.config.getMinimumTokens(this.ENGINE_ENUM) );
		this.initialized = true;
	}

	public getTargetPatterns(): Promise<TargetPattern[]> {
		return this.config.getTargetPatterns(this.ENGINE_ENUM);
	}

	public getCatalog(): Promise<Catalog>{
		const cpdCatalog = {
			rules: [{
				engine: this.ENGINE_NAME,
				sourcepackage: this.ENGINE_NAME,
				name: CpdRuleName,
				description: CpdRuleDescription,
				categories: [CpdRuleCategory],
				rulesets: [],
				isDfa: false,
				languages: CpdLanguagesSupported,
				defaultEnabled: true
			}],
			categories: [{
				engine: this.ENGINE_NAME,
				name: CpdRuleCategory,
				paths: []
			}],
			rulesets: []
		};
		return Promise.resolve(cpdCatalog);
	}

	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean {
		return true;
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {

		const languageToPaths = this.sortPaths(targets);
		const results: RuleResult[] = [];

		if (languageToPaths.size === 0) {
			this.logger.trace(`No matching cpd target files found. Nothing to execute.`);
			return [];
		}

		const ps: Promise<string>[] = [];
		// cpd only allows one language to be speificed at a time, so we must run it for each
		for (const language of Array.from(languageToPaths.keys())) {
			ps.push(this.runLanguage(language, languageToPaths.get(language)));
		}

		const runLanguageResults = await Promise.all(ps);
		for (const stdout of runLanguageResults) {
			results.push(...this.processStdOut(stdout));
		}

		this.logger.trace(`Found ${results.length} for CPD`);
		return results;
	}


	private sortPaths(targets: RuleTarget[]): Map<LANGUAGE, string[]> {
		const languageToPaths = new Map<LANGUAGE, string[]>();
		const unmatchedPaths: string[] = [];
		for (const target of targets) {
			for (const path of target.paths) {
				if (!this.matchPathToLanguage(path, languageToPaths)) {
					// If language could not be identified, note down the path
					unmatchedPaths.push(path);
				}
			}
		}

		// Let user know about file paths that could not be matched
		if (unmatchedPaths.length > 0) {
			uxEvents.emit(EVENTS.INFO_VERBOSE, eventMessages.getMessage('info.unmatchedPathExtensionCpd', [unmatchedPaths.join(",")]));
		}

		return languageToPaths;
	}

	/**
	 * Identify the language of a file using the file extension
	 * @param path to be examined
	 * @param languageToPaths map with entries of language to paths matched so far
	 * @returns true if the language was identifed and false if not
	 */
	private matchPathToLanguage(path: string, languageToPaths: Map<LANGUAGE, string[]>): boolean {
		const ext = path.slice(path.lastIndexOf(".") + 1);
		if (ext) {
			const language = FileExtToLanguage.get(ext.toLowerCase());
			if (language && CpdLanguagesSupported.includes(language)) {
				if (languageToPaths.has(language)) {
					languageToPaths.get(language).push(path);
				} else {
					languageToPaths.set(language, [path]);
				}
				return true;
			}
		}
		return false;
	}

	private runLanguage(language: string, targetPaths: string[]): Promise<string>{
		this.logger.trace(`About to run CPD (${language}, ${this.minimumTokens}). Targets: ${targetPaths.length}`);
		const selectedTargets = targetPaths.join(',');
		try {
			return CpdWrapper.execute(selectedTargets, language, this.minimumTokens);
		} catch (e) {
			const message: string = e instanceof Error ? e.message : e as string;
			this.logger.trace(`Cpd (${language}) evaluation failed: ` + message || e);
			throw new SfError(message);
		}
	}


	protected processStdOut(stdout: string): RuleResult[] {
		let ruleResults: RuleResult[] = [];

		this.logger.trace(`Output received from CPD: ${stdout}`);

		const cpdEnd = '</pmd-cpd>';
		const xmlStart = stdout.indexOf('<?xml');
		const xmlEnd = stdout.lastIndexOf(cpdEnd);
		if (xmlStart != -1 && xmlEnd != -1) {
			const cpdXml = stdout.slice(xmlStart, xmlEnd + cpdEnd.length);
			const cpdJson: Element = xml2js(cpdXml, {compact: false, ignoreDeclaration: true}) as Element;
			// Not all of the elements in this list will be duplication tags. We only want the ones that are.
			const duplications = (cpdJson.elements[0].elements || []).filter(element => element.name === "duplication");
			if (duplications) {
				ruleResults = this.jsonToRuleResults(duplications);
			}
		}

		if (ruleResults.length > 0) {
			this.logger.trace('Found rule violations.');
		}

		return ruleResults;
	}

	private jsonToRuleResults(duplications: Element[]): RuleResult[] {
		const ruleResults: RuleResult[] = [];

		for (const duplication of duplications) {

			const parts = duplication.elements;
			const codeFragment = parts.find(part => {return part.name === 'codefragment'}).elements[0].cdata;
			const occurences = parts.filter(part => {return part.name === 'file'});
			const codeFragmentID = crypto.createHash('md5').update(codeFragment).digest("hex").slice(0,7);
			// TODO: check for collisions (cpd can return multiple duplications for the same code fragment, so these have the same checksum currently)

			let occCount = 1;
			for (const occ of occurences) {
				// create a violation for each occurence of the code fragment
				const violation: RuleViolation = {
					line: occ.attributes.line as number,
					column: occ.attributes.column as number,
					endLine: occ.attributes.endline as number,
					endColumn: occ.attributes.endcolumn as number,
					ruleName: CpdRuleName,
					severity: CpdViolationSeverity,
					message: messages.getMessage("CpdViolationMessage", [codeFragmentID, occCount, occurences.length, duplication.attributes.lines, duplication.attributes.tokens]),
					category: CpdRuleCategory,
					url: CpdInfoUrl
				};
				occCount++;

				// if there are already violations for a file, add to its violations, if not, create a new RuleResult for that file.
				const ruleResult: RuleResult = ruleResults.find(ruleResult => {return occ.attributes.path === ruleResult.fileName})
				if (ruleResult) {
					ruleResult.violations.push(violation);
				} else {
					ruleResults.push(
						{
							engine: this.ENGINE_NAME,
							fileName: occ.attributes.path as string,
							violations: [violation]
						}
					);
				}
			}
		}
		return ruleResults;
	}

	public matchPath(path: string): boolean {
		this.logger.trace(`Engine CPD does not support custom rules: ${path}`);
		return false;
	}

	public async isEnabled(): Promise<boolean> {
		return await this.config.isEngineEnabled(this.ENGINE_ENUM);
	}

	/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
	public isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean {
		return engineUtils.isValueInFilter(this.getName(), filterValues);
	}

	public isDfaEngine(): boolean {
		return false;
	}

	public getNormalizedSeverity(severity: number): Severity{
		return severity;
	}

}

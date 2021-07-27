import {Logger, Messages, SfdxError} from '@salesforce/core';
import {xml2js} from 'xml-js';
import {Controller} from '../../Controller';
import {Catalog, Rule, RuleGroup, RuleResult, RuleTarget, RuleViolation, TargetPattern} from '../../types';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {Config} from '../util/Config';
import {ENGINE, LANGUAGE, Severity} from '../../Constants';
import * as engineUtils from '../util/CommonEngineUtils';
import CpdWrapper from './CpdWrapper';
import {FILE_EXTS_TO_LANGUAGE} from '../../Constants';
import {uxEvents} from '../ScannerEvents';
import crypto = require('crypto');


Messages.importMessagesDirectory(__dirname);
const messages = Messages.loadMessages('@salesforce/sfdx-scanner', 'CpdEngine');

export class CpdEngine extends AbstractRuleEngine {

	public readonly ENGINE_ENUM: ENGINE = ENGINE.CPD;
	public readonly ENGINE_NAME: string = ENGINE.CPD.valueOf();

	private minimumTokens: number;
	private logger: Logger;
	private config: Config;
	private initialized: boolean;
	private cpdCatalog: Catalog;
	private validCPDLanguages: LANGUAGE[];
	


	public getName(): string {
		return this.ENGINE_NAME;
	}

	public getTargetPatterns(): Promise<TargetPattern[]> {
		return this.config.getTargetPatterns(this.ENGINE_ENUM);
	}

	public getCatalog(): Promise<Catalog>{
		return Promise.resolve(this.cpdCatalog);
	}

	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	public shouldEngineRun(ruleGroups: RuleGroup[], rules: Rule[], target: RuleTarget[], engineOptions: Map<string, string>): boolean {
		return true;
	}
	
	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	public async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {

		const languageToPaths = this.sortPaths(targets);
		const results = [];

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
		const languageToPaths = new Map();

		for (const target of targets) {
			for (const path of target.paths) {
				const i = path.lastIndexOf(".");
				if (i === -1) {
					uxEvents.emit('warning-always', `Target: '${path}' was not processed by CPD, no file extension found.`);
					continue;
				}
				const ext = path.substr(i).toLowerCase();
				if (FILE_EXTS_TO_LANGUAGE.has(ext)) {
					if (this.validCPDLanguages.includes(FILE_EXTS_TO_LANGUAGE.get(ext))) {
						const language = FILE_EXTS_TO_LANGUAGE.get(ext);
						if (languageToPaths.has(language)) {
							languageToPaths.get(language).push(path);	
						} else {
							languageToPaths.set(language, [path]);
						}
					} else {
						uxEvents.emit('warning-always', `Target: '${path}' was not processed by CPD, language '${FILE_EXTS_TO_LANGUAGE.get(ext)}' not supported.`);
					}	
				} else {
					uxEvents.emit('warning-always', `Target: '${path}' was not processed by CPD, file extension '${ext}' not supported.`);
				}
			}
		}
		return languageToPaths;
	}

	private runLanguage(language: string, targetPaths: string[]): Promise<string>{
		this.logger.trace(`About to run CPD (${language}, ${this.minimumTokens}). Targets: ${targetPaths.length}`);
		const selectedTargets = targetPaths.join(',');
		try {
			const stdout = CpdWrapper.execute(selectedTargets, language, this.minimumTokens);
			return stdout;
		} catch (e) {
			this.logger.trace(`Cpd (${language}) evaluation failed: ` + (e.message || e));
			throw new SfdxError(e.message || e);
		}
	}


	protected processStdOut(stdout: string): RuleResult[] {
		let violations: RuleResult[] = [];

		this.logger.trace(`Output received from CPD: ${stdout}`);

		const cpdEnd = '</pmd-cpd>';
		const xmlStart = stdout.indexOf('<?xml');
		const xmlEnd = stdout.lastIndexOf(cpdEnd);
		if (xmlStart != -1 && xmlEnd != -1) {
			const cpdXml = stdout.slice(xmlStart, xmlEnd + cpdEnd.length);
			const cpdJson = xml2js(cpdXml, {compact: false, ignoreDeclaration: true}); 

			const duplications =  cpdJson.elements[0].elements;
			if (duplications) {
				violations = this.jsonToRuleResults(duplications);
			}
		}

		if (violations.length > 0) {
			this.logger.trace('Found rule violations.');
		}

		return violations;
	}

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	private jsonToRuleResults(duplications: any): RuleResult[] {   
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
					line: occ.attributes.line,
					column: occ.attributes.column,
					endLine: occ.attributes.endline,
					endColumn: occ.attributes.endcolumn,
					ruleName: this.cpdCatalog.rules[0].name,
					severity: Severity.LOW,
					message: messages.getMessage("CpdViolationMessage", [codeFragmentID, occCount, occurences.length, duplication.attributes.lines, duplication.attributes.tokens]),
					category: this.cpdCatalog.categories[0].name,
					url: 'https://pmd.github.io/latest/pmd_userdocs_cpd.html#refactoring-duplicates',
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
							fileName: occ.attributes.path,
							violations: [violation]
						}
					);
				}
			}
		}
		return ruleResults;
	}

	
	public async init(): Promise<void> {
		if (this.initialized) {
			return;
		}
		this.logger = await Logger.child(this.getName());
		this.config = await Controller.getConfig();
		this.initialized = true;
		this.logger = await Logger.child(this.getName())

		this.cpdCatalog = {
			rules: [{
				engine: this.ENGINE_ENUM.valueOf(),
				sourcepackage: this.ENGINE_ENUM.valueOf(),
				name: 'copy-paste-detected',
				description: 'Identify duplicate code blocks.',
				categories: ['Copy/Paste Detected'],
				rulesets: [],
				languages: await this.getLanguages(),
				defaultEnabled: true
			}],
			categories: [{
				engine: this.ENGINE_ENUM.valueOf(),
				name: 'Copy/Paste Detected',
				paths: []
			}],
			rulesets: []
		};

		this.minimumTokens = await this.config.getMinimumTokens(this.ENGINE_ENUM);
		this.initialized = true;
		this.validCPDLanguages = [LANGUAGE.APEX, LANGUAGE.JAVA, LANGUAGE.ECMASCRIPT, LANGUAGE.VISUALFORCE, LANGUAGE.XML];
	}

	private async getLanguages(): Promise<string[]> {
		const languages: Set<string> = new Set();
		for (const pattern of await this.config.getTargetPatterns(this.ENGINE_ENUM)){
			const ext = pattern.substr(pattern.lastIndexOf(".")).toLowerCase();
			if (FILE_EXTS_TO_LANGUAGE.has(ext)) {
				languages.add(FILE_EXTS_TO_LANGUAGE.get(ext));
			}
		}
		return Array.from(languages);
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


	public getNormalizedSeverity(severity: number): Severity{
		return severity;
	}



}

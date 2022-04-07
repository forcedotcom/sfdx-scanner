import { Catalog, RuleGroup, Rule, RuleTarget, RuleResult, RuleViolation, TargetPattern, ESRuleMetadata } from '../../types';
import {AbstractRuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE, EngineBase, Severity} from '../../Constants';
import {EslintProcessHelper, StaticDependencies, ProcessRuleViolationType} from './EslintCommons';
import {Logger, SfdxError} from '@salesforce/core';
import {EventCreator} from '../util/EventCreator';
import * as engineUtils from '../util/CommonEngineUtils';
import {ESLint, Linter} from 'eslint';

export class CustomEslintEngine extends AbstractRuleEngine {

	private dependencies: StaticDependencies;
	private helper: EslintProcessHelper;
	private eventCreator: EventCreator;
	protected logger: Logger;

	getEngine(): ENGINE {
		return ENGINE.ESLINT_CUSTOM;
	}

	getName(): string {
		return this.getEngine().valueOf();
	}

	isEngineRequested(filterValues: string[], engineOptions: Map<string, string>): boolean {
		return this.helper.isCustomRun(engineOptions)
		&& engineUtils.isFilterEmptyOrFilterValueStartsWith(EngineBase.ESLINT, filterValues);
	}

	getNormalizedSeverity(severity: number): Severity {
		switch (severity) {
			case 1:
				return Severity.MODERATE;
			case 2:
				return Severity.HIGH;
			default:
				return Severity.MODERATE;
		}
	}

	async init(dependencies = new StaticDependencies()): Promise<void> {
		this.logger = await Logger.child(`eslint-custom`);
		this.dependencies = dependencies;
		this.helper = new EslintProcessHelper();
		this.eventCreator = await EventCreator.create({});
	}

	async getTargetPatterns(): Promise<TargetPattern[]> {
		return Promise.resolve(["**"]); // TODO: We need a different way to set target pattern. Somehow eslintrc's ignore pattern doesn't work as expected
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

		return this.helper.isCustomRun(engineOptions)
			&& target.length > 0;
	}

	async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {

		const configFile = engineOptions.get(CUSTOM_CONFIG.EslintConfig);
		// No empty check needed because parameters are already validated

		const config = await this.extractConfig(configFile);

		// Let users know that they are on their own
		this.eventCreator.createUxInfoAlwaysMessage('info.customEslintHeadsUp', [configFile]);

		if (rules.length > 0) {
			this.eventCreator.createUxInfoAlwaysMessage('info.filtersIgnoredCustom', []);
		}
		// The config we loaded from the file should be treated as an override for whatever default configs exist.
		const eslint = this.dependencies.createESLint({overrideConfig: config});

		const results: RuleResult[] = [];
		for (const target of targets) {
			const esResults: ESLint.LintResult[] = await eslint.lintFiles(target.paths);

			const rulesMeta = eslint.getRulesMetaForResults(esResults);
			const rulesMap: Map<string,ESRuleMetadata> = new Map();
			Object.keys(rulesMeta).forEach(key => rulesMap.set(key, rulesMeta[key]));

			// Map results to supported format
			this.helper.addRuleResultsFromReport(this.getName(), results, esResults, rulesMap, this.processRuleViolation());
		}

		return results;
	}

	private async extractConfig(configFile: string): Promise<Linter.Config> {

		const fileHandler = this.dependencies.getFileHandler();
		if (!configFile || !(await fileHandler.exists(configFile))) {
			throw SfdxError.create('@salesforce/sfdx-scanner', 'CustomEslintEngine', 'ConfigFileDoesNotExist', [configFile]);
		}

		// At this point file exists. Convert content into JSON
		// TODO: handle yaml files
		const configContent = await fileHandler.readFile(configFile);

		// TODO: skim out comments in the file
		let config: Linter.Config;

		try {
			config = JSON.parse(configContent) as Linter.Config;
		} catch (error) {
			const message: string = error instanceof Error ? error.message : error as string;
			throw SfdxError.create('@salesforce/sfdx-scanner', 'CustomEslintEngine', 'InvalidJson', [configFile, message]);
		}

		return config;
	}

	processRuleViolation(): ProcessRuleViolationType {
		/* eslint-disable-next-line @typescript-eslint/no-unused-vars */
		return (fileName: string, ruleViolation: RuleViolation): void => {
			// do nothing - revisit when we have situations that need processing
		}
	}

	matchPath(path: string): boolean {
		throw new Error(`matchPath() - Method not implemented. Method mistakenly called with input ${path}`);
	}

	async isEnabled(): Promise<boolean> {
		// Hardcoding custom engines to be always enabled and not have a control point
		return Promise.resolve(true);
	}

}

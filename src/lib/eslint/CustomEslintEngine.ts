import { Catalog, RuleGroup, Rule, RuleTarget, RuleResult, RuleViolation } from '../../types';
import {RuleEngine} from '../services/RuleEngine';
import {CUSTOM_CONFIG, ENGINE} from '../../Constants';
import {EslintProcessHelper, StaticDependencies} from './EslintProcessHelper';
import { FileHandler } from '../util/FileHandler';
import {Logger, SfdxError} from '@salesforce/core';


export class CustomEslintEngine implements RuleEngine {
	private dependencies: StaticDependencies;
	private helper: EslintProcessHelper;
	protected logger: Logger;

	getEngine(): ENGINE {
		return ENGINE.ESLINT_CUSTOM;
	}

	getName(): string {
		return this.getEngine().valueOf();
	}

	isCustomConfigBased(): boolean {
		return true;
	}

	async init(): Promise<void> {
		this.logger = await Logger.child(`eslint-custom`);
		this.dependencies = new StaticDependencies();
		this.helper = new EslintProcessHelper();
	}

	async getTargetPatterns(): Promise<string[]> {
		return ["**/*.js"]; // TODO: We need a different way to set target pattern. Somehow eslintrc's ignore pattern doesn't work as expected
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

	async run(ruleGroups: RuleGroup[], rules: Rule[], targets: RuleTarget[], engineOptions: Map<string, string>): Promise<RuleResult[]> {
		// Ignoring ruleGroups, rules

		if (!this.helper.isCustomRun(engineOptions)) {
			this.logger.trace(`Not a custom run. No action needed`);
			return [];
		}

		const configFile = engineOptions.get(CUSTOM_CONFIG.EslintConfig);
		if (!configFile) {
			// TODO: this check is probably not needed since parameter check would've already happened
			throw new SfdxError(`Eslint config file value cannot be empty`);
		}

		if (!targets || targets.length === 0) {
			this.logger.trace(`No matching targets for CustomEslintEngine`);
			return [];
		}

		const config = await this.extractConfig(configFile);

		const cli = this.dependencies.createCLIEngine(config);

		const results: RuleResult[] = [];
		for (const target of targets) {
			const report = cli.executeOnFiles(target.paths);

			// Map results to supported format
			this.helper.addRuleResultsFromReport(this.getName(), results, report, cli.getRules(), this.processRuleViolation);
		}
		
		return results;
	}

	
	/* eslint-disable @typescript-eslint/no-explicit-any */
	private async extractConfig(configFile: string): Promise<Record<string, any>> {
		
		const fileHandler = new FileHandler();
		if (!configFile || !(await fileHandler.exists(configFile))) {
			throw new SfdxError(`Invalid file provided as eslint configuration: ${configFile}`);
		}

		// At this point file exists. Convert content into JSON
		// TODO: handle yaml files
		const configContent = await fileHandler.readFile(configFile);

		// TODO: skim out comments in the file
		let config;

		try {
			config = JSON.parse(configContent);
		} catch (error) {
			throw new SfdxError(`Invalid config file ${configFile} - Could not read JSON: ${error || error.message}`);
		}
		
		return config;
	}

	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	processRuleViolation(fileName: string, ruleViolation: RuleViolation): void {
		// do nothing - revisit when we have situations that need processing
	}

	/* eslint-disable-next-line no-unused-vars, @typescript-eslint/no-unused-vars */
	matchPath(path: string): boolean {
		throw new Error('matchPath() - Method not implemented.');
	}

	async isEnabled(): Promise<boolean> {
		// Hardcoding custom engines to be always enabled and not have a control point
		return Promise.resolve(true);
	}

}
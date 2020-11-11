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
		return ENGINE.ESLINT_TYPESCRIPT;
	}

	getName(): string {
		return this.getEngine().valueOf();
	}

	async init(): Promise<void> {
		this.logger = await Logger.child(`eslint-custom`);
		this.dependencies = new StaticDependencies();
		this.helper = new EslintProcessHelper();
	}

	async getTargetPatterns(): Promise<string[]> {
		return ["**"]; // TODO: crude code. revisit
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

	
	private async extractConfig(configFile: string) {
		
		const fileHandler = new FileHandler();
		if (!configFile || !(await fileHandler.exists(configFile))) {
			throw new SfdxError(`Invalid file provided as eslint configuration: ${configFile}`);
		}

		// At this point file exists. Convert content into JSON
		// TODO: handle yaml files
		const configContent = await fileHandler.readFile(configFile);

		// TODO: skim out comments in the file
		const config = JSON.parse(configContent);
		return config;
	}

	processRuleViolation(fileName: string, ruleViolation: RuleViolation): void {
		// do nothing for now - TODO: revisit
	}

	matchPath(path: string): boolean {
		throw new Error('matchPath() - Method not implemented.');
	}
	async isEnabled(): Promise<boolean> {
		return true; // TODO: Is this applicable?
	}

}
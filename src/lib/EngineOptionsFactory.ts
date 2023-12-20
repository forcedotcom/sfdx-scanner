import {LooseObject, SfgeConfig} from "../types";
import {CUSTOM_CONFIG} from "../Constants";
import {PathFactory} from "./PathFactory";
import {TYPESCRIPT_ENGINE_OPTIONS} from "./eslint/TypescriptEslintStrategy";
import {Messages, SfError} from "@salesforce/core";
import {INTERNAL_ERROR_CODE} from "./ScannerRunCommand";
import normalize = require('normalize-path');
import untildify = require("untildify");

// TODO: Move this to some messages helper class
const runMessages: Messages<string> = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-pathless');

export interface EngineOptionsFactory {
	createEngineOptions(inputs: LooseObject): Map<string,string>;
}

abstract class CommonEngineOptionsFactory implements EngineOptionsFactory {
	private readonly pathFactory: PathFactory;

	protected constructor(pathFactory: PathFactory) {
		this.pathFactory = pathFactory;
	}

	createEngineOptions(inputs: LooseObject): Map<string, string> {
		const options: Map<string,string> = new Map();

		// We should only add a GraphEngine config if we were given a --projectdir flag.
		let projectDirPaths: string[] = this.pathFactory.createProjectDirPaths(inputs);
		if (projectDirPaths.length > 0) {
			const sfgeConfig: SfgeConfig = {
				projectDirs: projectDirPaths
			};
			options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
		}

		return options;
	}

}

export class RunEngineOptionsFactory extends CommonEngineOptionsFactory {
	public constructor(pathFactory: PathFactory) {
		super(pathFactory);
	}

	public override createEngineOptions(inputs: LooseObject): Map<string, string> {
		const options: Map<string, string> = super.createEngineOptions(inputs);

		if (inputs.tsconfig) {
			const tsconfig = normalize(untildify(inputs.tsconfig as string));
			options.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, tsconfig);
		}

		// TODO: This fix for W-7791882 is suboptimal, because it leaks our abstractions and pollutes the command with
		//  engine-specific flags. Replace it in 3.0.
		if (inputs.env) {
			try {
				const parsedEnv: LooseObject = JSON.parse(inputs.env as string) as LooseObject;
				options.set('env', JSON.stringify(parsedEnv));
			} catch (e) {
				throw new SfError(runMessages.getMessage('output.invalidEnvJson'), null, null, INTERNAL_ERROR_CODE);
			}
		}

		// Capturing eslintconfig value, if provided
		if (inputs.eslintconfig) {
			const eslintConfig = normalize(untildify(inputs.eslintconfig as string));
			options.set(CUSTOM_CONFIG.EslintConfig, eslintConfig);
		}

		// Capturing pmdconfig value, if provided
		if (inputs.pmdconfig) {
			const pmdConfig = normalize(untildify(inputs.pmdconfig as string));
			options.set(CUSTOM_CONFIG.PmdConfig, pmdConfig);
		}

		// Capturing verbose-violations flag value (used for RetireJS output)
		if (inputs["verbose-violations"]) {
			options.set(CUSTOM_CONFIG.VerboseViolations, "true");
		}

		return options;
	}
}

export class RunDfaEngineOptionsFactory extends CommonEngineOptionsFactory {
	public constructor(pathFactory: PathFactory) {
		super(pathFactory);
	}

	public override createEngineOptions(inputs: LooseObject): Map<string,string> {
		const options: Map<string, string> = super.createEngineOptions(inputs);

		// The flags have been validated by now, meaning --projectdir is confirmed as present,
		// meaning we can assume the existence of a GraphEngine config in the common options.
		const sfgeConfig: SfgeConfig = JSON.parse(options.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig;
		if (inputs['rule-thread-count'] != null) {
			sfgeConfig.ruleThreadCount = inputs['rule-thread-count'] as number;
		}
		if (inputs['rule-thread-timeout'] != null) {
			sfgeConfig.ruleThreadTimeout = inputs['rule-thread-timeout'] as number;
		}
		if (inputs['sfgejvmargs'] != null) {
			sfgeConfig.jvmArgs = inputs['sfgejvmargs'] as string;
		}
		if (inputs['pathexplimit'] != null) {
			sfgeConfig.pathexplimit = inputs['pathexplimit'] as number;
		}
		sfgeConfig.ruleDisableWarningViolation = getBooleanEngineOption(inputs, RULE_DISABLE_WARNING_VIOLATION_FLAG);
		options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));

		return options;
	}
}


const RULE_DISABLE_WARNING_VIOLATION_FLAG: string = 'rule-disable-warning-violation';
const RULE_DISABLE_WARNING_VIOLATION_ENVVAR: string = 'SFGE_RULE_DISABLE_WARNING_VIOLATION';
const BOOLEAN_ENVARS_BY_FLAG: Map<string,string> = new Map([
	[RULE_DISABLE_WARNING_VIOLATION_FLAG, RULE_DISABLE_WARNING_VIOLATION_ENVVAR]
]);

/**
 * Boolean flags cannot automatically inherit their value from environment variables. Instead, we use this
 * method to handle that inheritance if necessary.
 * @param flag - The name of a boolean flag associated with this command
 * @returns true if the flag is set or the associated env-var is set to "true"; else false.
 * @protected
 */
function getBooleanEngineOption(inputs: LooseObject, flagName: string): boolean {
	// Check the status of the flag first, since the flag being true should trump the environment variable's value.
	if (inputs[flagName] != null) {
		return inputs[flagName] as boolean;
	}
	// If the flag isn't set, get the name of the corresponding environment variable and check its value.
	const envVar = BOOLEAN_ENVARS_BY_FLAG.get(flagName);
	return envVar in process.env && process.env[envVar].toLowerCase() === 'true';
}

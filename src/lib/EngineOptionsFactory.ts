import {Inputs, LooseObject, SfgeConfig} from "../types";
import {CUSTOM_CONFIG, ENGINE, INTERNAL_ERROR_CODE} from "../Constants";
import {InputProcessor} from "./InputProcessor";
import {TYPESCRIPT_ENGINE_OPTIONS} from "./eslint/TypescriptEslintStrategy";
import {SfError} from "@salesforce/core";
import normalize = require('normalize-path');
import untildify = require("untildify");
import {BundleName, getMessage} from "../MessageCatalog";
import {EngineOptions} from "./RuleManager";

/**
 * Service for processing inputs to create EngineOptions
 */
export interface EngineOptionsFactory {
	createEngineOptions(inputs: Inputs): Map<string,string>;
}

abstract class CommonEngineOptionsFactory implements EngineOptionsFactory {
	private readonly inputProcessor: InputProcessor;

	protected constructor(inputProcessor: InputProcessor) {
		this.inputProcessor = inputProcessor;
	}

	protected abstract shouldSfgeRun(inputs: Inputs): boolean;

	createEngineOptions(inputs: Inputs): EngineOptions {
		const options: Map<string,string> = new Map();
		if (this.shouldSfgeRun(inputs)) {
			const sfgeConfig: SfgeConfig = {
				projectDirs: this.inputProcessor.resolveProjectDirPaths(inputs)
			};
			options.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));
		}
		return options;
	}

}

export class RunEngineOptionsFactory extends CommonEngineOptionsFactory {
	public constructor(inputProcessor: InputProcessor) {
		super(inputProcessor);
	}

	protected shouldSfgeRun(inputs: Inputs): boolean {
		return inputs.engine && (inputs.engine as string[]).includes(ENGINE.SFGE);
	}

	public override createEngineOptions(inputs: Inputs): EngineOptions {
		const engineOptions: EngineOptions = super.createEngineOptions(inputs);

		if (inputs.tsconfig) {
			const tsconfig = normalize(untildify(inputs.tsconfig as string));
			engineOptions.set(TYPESCRIPT_ENGINE_OPTIONS.TSCONFIG, tsconfig);
		}

		// TODO: This fix for W-7791882 is suboptimal, because it leaks our abstractions and pollutes the command with
		//  engine-specific flags. Replace it in 3.0.
		if (inputs.env) {
			try {
				const parsedEnv: LooseObject = JSON.parse(inputs.env as string) as LooseObject;
				engineOptions.set('env', JSON.stringify(parsedEnv));
			} catch (e) {
				throw new SfError(getMessage(BundleName.Run, 'output.invalidEnvJson'), null, null, INTERNAL_ERROR_CODE);
			}
		}

		// Capturing eslintconfig value, if provided
		if (inputs.eslintconfig) {
			const eslintConfig = normalize(untildify(inputs.eslintconfig as string));
			engineOptions.set(CUSTOM_CONFIG.EslintConfig, eslintConfig);
		}

		// Capturing pmdconfig value, if provided
		if (inputs.pmdconfig) {
			const pmdConfig = normalize(untildify(inputs.pmdconfig as string));
			engineOptions.set(CUSTOM_CONFIG.PmdConfig, pmdConfig);
		}

		// Capturing verbose-violations flag value (used for RetireJS output)
		if (inputs["verbose-violations"]) {
			engineOptions.set(CUSTOM_CONFIG.VerboseViolations, "true");
		}

		return engineOptions;
	}
}

export class RunDfaEngineOptionsFactory extends CommonEngineOptionsFactory {
	public constructor(inputProcessor: InputProcessor) {
		super(inputProcessor);
	}

	// eslint-disable-next-line @typescript-eslint/no-unused-vars
	protected shouldSfgeRun(_inputs: Inputs): boolean {
		return true;
	}

	public override createEngineOptions(inputs: Inputs): EngineOptions {
		const engineOptions: EngineOptions = super.createEngineOptions(inputs);

		// The flags have been validated by now, meaning --projectdir is confirmed as present,
		// meaning we can assume the existence of a GraphEngine config in the common options.
		const sfgeConfig: SfgeConfig = JSON.parse(engineOptions.get(CUSTOM_CONFIG.SfgeConfig)) as SfgeConfig;
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
		if (inputs['enablecaching'] != null) {
			sfgeConfig.enablecaching = inputs['enablecaching'] as boolean;
		}
		if (inputs['cachepath'] != null) {
			sfgeConfig.cachepath = inputs['cachepath'] as string;
		}
		sfgeConfig.ruleDisableWarningViolation = getBooleanEngineOption(inputs, RULE_DISABLE_WARNING_VIOLATION_FLAG);
		engineOptions.set(CUSTOM_CONFIG.SfgeConfig, JSON.stringify(sfgeConfig));

		return engineOptions;
	}
}


const RULE_DISABLE_WARNING_VIOLATION_FLAG = 'rule-disable-warning-violation';
const RULE_DISABLE_WARNING_VIOLATION_ENVVAR = 'SFGE_RULE_DISABLE_WARNING_VIOLATION';
const BOOLEAN_ENVARS_BY_FLAG: Map<string, string> = new Map([
	[RULE_DISABLE_WARNING_VIOLATION_FLAG, RULE_DISABLE_WARNING_VIOLATION_ENVVAR]
]);

/**
 * Boolean flags cannot automatically inherit their value from environment variables. Instead, we use this
 * method to handle that inheritance if necessary.
 * @param flag - The name of a boolean flag associated with this command
 * @returns true if the flag is set or the associated env-var is set to "true"; else false.
 */
function getBooleanEngineOption(inputs: Inputs, flagName: string): boolean {
	// Check the status of the flag first, since the flag being true should trump the environment variable's value.
	if (inputs[flagName] != null) {
		return inputs[flagName] as boolean;
	}
	// If the flag isn't set, get the name of the corresponding environment variable and check its value.
	const envVar = BOOLEAN_ENVARS_BY_FLAG.get(flagName);
	return envVar in process.env && process.env[envVar].toLowerCase() === 'true';
}

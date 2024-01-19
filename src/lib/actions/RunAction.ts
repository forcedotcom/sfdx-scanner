import {AbstractRunAction} from "./AbstractRunAction";
import {Display} from "../Display";
import {InputProcessor} from "../InputProcessor";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {Inputs} from "../../types";
import {Logger, SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import {RuleFilterFactory} from "../RuleFilterFactory";
import {ResultsProcessorFactory} from "../output/ResultsProcessorFactory";

/**
 * The Action behind the "run" command
 */
export class RunAction extends AbstractRunAction {
	public constructor(logger: Logger, display: Display, inputProcessor: InputProcessor,
						ruleFilterFactory: RuleFilterFactory, engineOptionsFactory: EngineOptionsFactory,
						resultsProcessorFactory: ResultsProcessorFactory) {
		super(logger, display, inputProcessor, ruleFilterFactory, engineOptionsFactory, resultsProcessorFactory);
	}

	public override async validateInputs(inputs: Inputs): Promise<void> {
		await super.validateInputs(inputs);

		if (inputs.tsconfig && inputs.eslintconfig) {
			throw new SfError(getMessage(BundleName.Run, 'validations.tsConfigEslintConfigExclusive'));
		}

		if ((inputs.pmdconfig || inputs.eslintconfig) && (inputs.category || inputs.ruleset)) {
			this.display.displayInfo(getMessage(BundleName.Run, 'output.filtersIgnoredCustom', []));
		}
		// None of the pathless engines support method-level targeting, so attempting to use it should result in an error.
		if (inputs.target) {
			for (const target of (inputs.target as string[])) {
				if (target.indexOf('#') > -1) {
					throw new SfError(getMessage(BundleName.Run, 'validations.methodLevelTargetingDisallowed', [target]));
				}
			}
		}
	}

	protected isDfa(): boolean {
		return false;
	}
}

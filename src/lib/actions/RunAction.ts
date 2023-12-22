import {AbstractRunAction} from "./AbstractRunAction";
import {Display} from "../Display";
import {InputsResolver} from "../InputsResolver";
import {RunOptionsFactory} from "../RunOptionsFactory";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {Inputs} from "../../types";
import {SfError} from "@salesforce/core";
import {Bundle, getMessage} from "../../MessageCatalog";

/**
 * The Action behind the "run" command
 */
export class RunAction extends AbstractRunAction {
	public constructor(display: Display, inputsResolver: InputsResolver, runOptionsFactory: RunOptionsFactory,
						engineOptionsFactory: EngineOptionsFactory) {
		super(display, inputsResolver, runOptionsFactory, engineOptionsFactory);
	}

	public override async validateInputs(inputs: Inputs): Promise<void> {
		await super.validateInputs(inputs);

		if (inputs.tsconfig && inputs.eslintconfig) {
			throw new SfError(getMessage(Bundle.Run, 'validations.tsConfigEslintConfigExclusive'));
		}

		if ((inputs.pmdconfig || inputs.eslintconfig) && (inputs.category || inputs.ruleset)) {
			this.display.displayInfo(getMessage(Bundle.Run, 'output.filtersIgnoredCustom', []));
		}
		// None of the pathless engines support method-level targeting, so attempting to use it should result in an error.
		for (const target of (inputs.target as string[])) {
			if (target.indexOf('#') > -1) {
				throw new SfError(getMessage(Bundle.Run, 'validations.methodLevelTargetingDisallowed', [target]));
			}
		}
	}
}

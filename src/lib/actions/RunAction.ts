import {AbstractRunAction} from "./AbstractRunAction";
import {Display} from "../Display";
import {PathResolver} from "../PathResolver";
import {RunOptionsFactory} from "../RunOptionsFactory";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {Inputs} from "../../types";
import {SfError} from "@salesforce/core";
import {Bundle, getMessage} from "../../MessageCatalog";

export class RunAction extends AbstractRunAction {
	public constructor(display: Display, pathResolver: PathResolver, runOptionsFactory: RunOptionsFactory,
					   engineOptionsFactory: EngineOptionsFactory) {
		super(display, pathResolver, runOptionsFactory, engineOptionsFactory);
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

import {AbstractRunAction} from "./AbstractRunAction";
import {Display} from "../Display";
import {InputsResolver} from "../InputsResolver";
import {RuleFilterFactory} from "../RuleFilterFactory";
import {RunOptionsFactory} from "../RunOptionsFactory";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {Inputs} from "../../types";
import {FileHandler} from "../util/FileHandler";
import {Logger, SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import * as globby from "globby";

/**
 * The Action behind the "run dfa" command
 */
export class RunDfaAction extends AbstractRunAction {
	public constructor(logger: Logger, display: Display, inputsResolver: InputsResolver, ruleFilterFactory: RuleFilterFactory,
						runOptionsFactory: RunOptionsFactory, engineOptionsFactory: EngineOptionsFactory) {
		super(logger, display, inputsResolver, ruleFilterFactory, runOptionsFactory, engineOptionsFactory);
	}

	public override async validateInputs(inputs: Inputs): Promise<void> {
		await super.validateInputs(inputs);

		const fh = new FileHandler();
		// The superclass will validate that --projectdir is well-formed,
		// but doesn't require that the flag actually be present.
		// So we should make sure it exists here.
		if (!inputs.projectdir || (inputs.projectdir as string[]).length === 0) {
			throw new SfError(getMessage(BundleName.RunDfa, 'validations.projectdirIsRequired'));
		}
		// Entries in the target array may specify methods, but only if the entry is neither a directory nor a glob.
		for (const target of (inputs.target as string[])) {
			// The target specifies a method if it includes the `#` syntax.
			if (target.indexOf('#') > -1) {
				if(globby.hasMagic(target)) {
					throw new SfError(getMessage(BundleName.RunDfa, 'validations.methodLevelTargetCannotBeGlob'));
				}
				const potentialFilePath = target.split('#')[0];
				if (!(await fh.isFile(potentialFilePath))) {
					throw new SfError(getMessage(BundleName.RunDfa, 'validations.methodLevelTargetMustBeRealFile', [potentialFilePath]));
				}
			}
		}
	}
}

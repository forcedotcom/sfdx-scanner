import {AbstractRunAction} from "./AbstractRunAction";
import {Display} from "../Display";
import {InputProcessor} from "../InputProcessor";
import {RuleFilterFactory} from "../RuleFilterFactory";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {Inputs} from "../../types";
import {FileHandler} from "../util/FileHandler";
import {Logger, SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import * as globby from "globby";
import {ResultsProcessorFactory} from "../output/ResultsProcessorFactory";

/**
 * The Action behind the "run dfa" command
 */
export class RunDfaAction extends AbstractRunAction {
	public constructor(logger: Logger, display: Display, inputProcessor: InputProcessor,
						ruleFilterFactory: RuleFilterFactory, engineOptionsFactory: EngineOptionsFactory,
						resultsProcessorFactory: ResultsProcessorFactory) {
		super(logger, display, inputProcessor, ruleFilterFactory, engineOptionsFactory, resultsProcessorFactory);
	}

	public override async validateInputs(inputs: Inputs): Promise<void> {
		await super.validateInputs(inputs);

		const fh = new FileHandler();
		// Entries in the target array may specify methods, but only if the entry is neither a directory nor a glob.
		if (inputs.target) {
			for (const target of (inputs.target as string[])) {
				// The target specifies a method if it includes the `#` syntax.
				if (target.indexOf('#') > -1) {
					if (globby.hasMagic(target)) {
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

	protected isDfa(): boolean {
		return true;
	}
}

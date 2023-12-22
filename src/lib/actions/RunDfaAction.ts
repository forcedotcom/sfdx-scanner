import {AbstractRunAction} from "./AbstractRunAction";
import {Display} from "../Display";
import {PathResolver} from "../PathResolver";
import {RunOptionsFactory} from "../RunOptionsFactory";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {Inputs} from "../../types";
import {FileHandler} from "../util/FileHandler";
import {SfError} from "@salesforce/core";
import {Bundle, getMessage} from "../../MessageCatalog";
import * as globby from "globby";

export class RunDfaAction extends AbstractRunAction {
	public constructor(display: Display, pathResolver: PathResolver, runOptionsFactory: RunOptionsFactory,
					   engineOptionsFactory: EngineOptionsFactory) {
		super(display, pathResolver, runOptionsFactory, engineOptionsFactory);
	}

	public override async validateInputs(inputs: Inputs): Promise<void> {
		await super.validateInputs(inputs);

		const fh = new FileHandler();
		// The superclass will validate that --projectdir is well-formed,
		// but doesn't require that the flag actually be present.
		// So we should make sure it exists here.
		if (!inputs.projectdir || (inputs.projectdir as string[]).length === 0) {
			throw new SfError(getMessage(Bundle.RunDfa, 'validations.projectdirIsRequired'));
		}
		// Entries in the target array may specify methods, but only if the entry is neither a directory nor a glob.
		for (const target of (inputs.target as string[])) {
			// The target specifies a method if it includes the `#` syntax.
			if (target.indexOf('#') > -1) {
				if(globby.hasMagic(target)) {
					throw new SfError(getMessage(Bundle.RunDfa, 'validations.methodLevelTargetCannotBeGlob'));
				}
				const potentialFilePath = target.split('#')[0];
				if (!(await fh.isFile(potentialFilePath))) {
					throw new SfError(getMessage(Bundle.RunDfa, 'validations.methodLevelTargetMustBeRealFile', [potentialFilePath]));
				}
			}
		}
	}
}

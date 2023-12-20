import {LooseObject} from "../types";
import {FileHandler} from "./util/FileHandler";
import {Messages, SfError} from "@salesforce/core";
import {OUTPUT_FORMAT} from "./RuleManager";
import globby = require("globby");
import {inferFormatFromOutfile} from "./RunOptionsFactory";
import {Loggable} from "./Loggable";

export interface InputValidator {
	validate(inputs: LooseObject): Promise<void>;
}

// TODO: Wrap these in some message fetching class
const commonRunMessages: Messages<string> = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');
const runMessages: Messages<string> = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-pathless');
const runDfaMessages: Messages<string> = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-dfa');

abstract class CommonRunCommandInputValidator implements InputValidator {
	protected readonly uxLogger: Loggable;

	protected constructor(uxLogger: Loggable) {
		this.uxLogger = uxLogger;
	}

	public async validate(inputs: LooseObject): Promise<void> {
		const fh = new FileHandler();
		// If there's a --projectdir flag, its entries must be non-glob paths pointing to existing directories.
		if (inputs.projectdir) {
			for (const dir of (inputs.projectdir as string[])) { // TODO: MOVE AWAY FROM ALLOWING AN ARRAY OF DIRECTORIES HERE AND ERROR IF THERE IS MORE THAN ONE DIRECTORY
				if (globby.hasMagic(dir)) {
					throw new SfError(commonRunMessages.getMessage('validations.projectdirCannotBeGlob', []));
				} else if (!(await fh.exists(dir))) {
					throw new SfError(commonRunMessages.getMessage('validations.projectdirMustExist', []));
				} else if (!(await fh.stats(dir)).isDirectory()) {
					throw new SfError(commonRunMessages.getMessage('validations.projectdirMustBeDir', []));
				}
			}
		}
		// If the user explicitly specified both a format and an outfile, we need to do a bit of validation there.
		if (inputs.format && inputs.outfile) {
			const inferredOutfileFormat: OUTPUT_FORMAT = inferFormatFromOutfile(inputs.outfile);
			// For the purposes of this validation, we treat junit as xml.
			const chosenFormat: string = inputs.format === 'junit' ? 'xml' : inputs.format as string;
			// If the chosen format is TABLE, we immediately need to exit. There's no way to sensibly write the output
			// of TABLE to a file.
			if (chosenFormat === OUTPUT_FORMAT.TABLE) {
				throw new SfError(commonRunMessages.getMessage('validations.cannotWriteTableToFile', []));
			}
			// Otherwise, we want to be liberal with the user. If the chosen format doesn't match the outfile's extension,
			// just log a message saying so.
			if (chosenFormat !== inferredOutfileFormat) {
				this.uxLogger.log(commonRunMessages.getMessage('validations.outfileFormatMismatch', [inputs.format as string, inferredOutfileFormat]));
			}
		}
	}
}

export class RunCommandInputValidator extends CommonRunCommandInputValidator {
	public constructor(uxLogger: Loggable) {
		super(uxLogger);
	}

	public override async validate(inputs: LooseObject): Promise<void> {
		await super.validate(inputs);

		if (inputs.tsconfig && inputs.eslintconfig) {
			throw new SfError(runMessages.getMessage('validations.tsConfigEslintConfigExclusive'));
		}

		if ((inputs.pmdconfig || inputs.eslintconfig) && (inputs.category || inputs.ruleset)) {
			this.uxLogger.log(runMessages.getMessage('output.filtersIgnoredCustom', []));
		}
		// None of the pathless engines support method-level targeting, so attempting to use it should result in an error.
		for (const target of (inputs.target as string[])) {
			if (target.indexOf('#') > -1) {
				throw new SfError(runMessages.getMessage('validations.methodLevelTargetingDisallowed', [target]));
			}
		}
	}
}

export class RunDfaCommandInputValidator extends CommonRunCommandInputValidator {
	public constructor(uxLogger: Loggable) {
		super(uxLogger);
	}

	public override async validate(inputs: LooseObject): Promise<void> {
		await super.validate(inputs);

		const fh = new FileHandler();
		// The superclass will validate that --projectdir is well-formed,
		// but doesn't require that the flag actually be present.
		// So we should make sure it exists here.
		if (!inputs.projectdir || (inputs.projectdir as string[]).length === 0) {
			throw new SfError(runDfaMessages.getMessage('validations.projectdirIsRequired'));
		}
		// Entries in the target array may specify methods, but only if the entry is neither a directory nor a glob.
		for (const target of (inputs.target as string[])) {
			// The target specifies a method if it includes the `#` syntax.
			if (target.indexOf('#') > -1) {
				if(globby.hasMagic(target)) {
					throw new SfError(runDfaMessages.getMessage('validations.methodLevelTargetCannotBeGlob'));
				}
				const potentialFilePath = target.split('#')[0];
				if (!(await fh.isFile(potentialFilePath))) {
					throw new SfError(runDfaMessages.getMessage('validations.methodLevelTargetMustBeRealFile', [potentialFilePath]));
				}
			}
		}
	}
}

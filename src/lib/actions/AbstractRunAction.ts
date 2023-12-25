import {Action} from "../ScannerCommand";
import {Inputs, RecombinedRuleResults} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {FileHandler} from "../util/FileHandler";
import {SfError} from "@salesforce/core";
import {Bundle, getMessage} from "../../MessageCatalog";
import {EngineOptions, OUTPUT_FORMAT, RuleManager, RunOptions} from "../RuleManager";
import {inferFormatFromOutfile, RunOptionsFactory} from "../RunOptionsFactory";
import * as globby from "globby";
import {Display} from "../Display";
import {RuleFilter} from "../RuleFilter";
import {RuleFilterFactoryImpl} from "../RuleFilterFactory";
import {Controller} from "../../Controller";
import {RunOutputOptions, RunOutputProcessor} from "../util/RunOutputProcessor";
import {InputsResolver} from "../InputsResolver";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {INTERNAL_ERROR_CODE} from "../../Constants";

/**
 * Abstract Action to share a common implementation behind the "run" and "run dfa" commands
 */
export abstract class AbstractRunAction implements Action {
	protected readonly display: Display;
	private readonly inputsResolver: InputsResolver;
	private readonly runOptionsFactory: RunOptionsFactory;
	private readonly engineOptionsFactory: EngineOptionsFactory;

	protected constructor(display: Display, inputsResolver: InputsResolver, runOptionsFactory: RunOptionsFactory,
							engineOptionsFactory: EngineOptionsFactory) {
		this.display = display;
		this.inputsResolver = inputsResolver;
		this.runOptionsFactory = runOptionsFactory;
		this.engineOptionsFactory = engineOptionsFactory;
	}

	async validateInputs(inputs: Inputs): Promise<void> {
		const fh = new FileHandler();
		// If there's a --projectdir flag, its entries must be non-glob paths pointing to existing directories.
		if (inputs.projectdir) {
			// TODO: MOVE AWAY FROM ALLOWING AN ARRAY OF DIRECTORIES HERE AND ERROR IF THERE IS MORE THAN ONE DIRECTORY
			for (const dir of (inputs.projectdir as string[])) {
				if (globby.hasMagic(dir)) {
					throw new SfError(getMessage(Bundle.CommonRun, 'validations.projectdirCannotBeGlob'));
				} else if (!(await fh.exists(dir))) {
					throw new SfError(getMessage(Bundle.CommonRun, 'validations.projectdirMustExist'));
				} else if (!(await fh.stats(dir)).isDirectory()) {
					throw new SfError(getMessage(Bundle.CommonRun, 'validations.projectdirMustBeDir'));
				}
			}
		}
		// If the user explicitly specified both a format and an outfile, we need to do a bit of validation there.
		if (inputs.format && inputs.outfile) {
			const inferredOutfileFormat: OUTPUT_FORMAT = inferFormatFromOutfile(inputs.outfile as string);
			// For the purposes of this validation, we treat junit as xml.
			const chosenFormat: string = inputs.format === 'junit' ? 'xml' : inputs.format as string;
			// If the chosen format is TABLE, we immediately need to exit. There's no way to sensibly write the output
			// of TABLE to a file.
			if (chosenFormat === OUTPUT_FORMAT.TABLE) {
				throw new SfError(getMessage(Bundle.CommonRun, 'validations.cannotWriteTableToFile', []));
			}
			// Otherwise, we want to be liberal with the user. If the chosen format doesn't match the outfile's extension,
			// just log a message saying so.
			if (chosenFormat !== inferredOutfileFormat) {
				this.display.displayInfo(getMessage(Bundle.CommonRun, 'validations.outfileFormatMismatch', [inputs.format as string, inferredOutfileFormat]));
			}
		}
	}

	async run(inputs: Inputs): Promise<AnyJson> {
		const filters: RuleFilter[] = new RuleFilterFactoryImpl().createRuleFilters(inputs);
		const targetPaths: string[] = this.inputsResolver.resolveTargetPaths(inputs);
		const runOptions: RunOptions = this.runOptionsFactory.createRunOptions(inputs);
		const engineOptions: EngineOptions = this.engineOptionsFactory.createEngineOptions(inputs);

		// TODO: Inject RuleManager as a dependency to improve testability by removing coupling to runtime implementation
		const ruleManager: RuleManager = await Controller.createRuleManager();

		let output: RecombinedRuleResults = null;
		try {
			output = await ruleManager.runRulesMatchingCriteria(filters, targetPaths, runOptions, engineOptions);
		} catch (e) {
			// Rethrow any errors as SF errors.
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfError(message, null, null, INTERNAL_ERROR_CODE);
		}

		const outputOptions: RunOutputOptions = {
			format: runOptions.format,
			severityForError: inputs['severity-threshold'] as number,
			outfile: inputs.outfile as string
		};
		return new RunOutputProcessor(this.display, outputOptions).processRunOutput(output);
	}
}

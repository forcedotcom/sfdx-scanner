import {Action} from "../ScannerCommand";
import {FormattedOutput, Inputs, RecombinedRuleResults} from "../../types";
import {AnyJson} from "@salesforce/ts-types";
import {FileHandler} from "../util/FileHandler";
import {Logger, SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import {EngineOptions, OUTPUT_FORMAT, RuleManager, RunOptions} from "../RuleManager";
import {inferFormatFromOutfile, RunOptionsFactory} from "../RunOptionsFactory";
import * as globby from "globby";
import {Display} from "../Display";
import {RuleFilter} from "../RuleFilter";
import {RuleFilterFactory} from "../RuleFilterFactory";
import {Controller} from "../../Controller";
import {RunOutputOptions, RunOutputProcessor} from "../util/RunOutputProcessor";
import {InputsResolver} from "../InputsResolver";
import {EngineOptionsFactory} from "../EngineOptionsFactory";
import {CUSTOM_CONFIG, INTERNAL_ERROR_CODE} from "../../Constants";
import {Results} from "../output/Results";

/**
 * Abstract Action to share a common implementation behind the "run" and "run dfa" commands
 */
export abstract class AbstractRunAction implements Action {
	private readonly logger: Logger;
	protected readonly display: Display;
	private readonly inputsResolver: InputsResolver;
	private readonly ruleFilterFactory: RuleFilterFactory;
	private readonly runOptionsFactory: RunOptionsFactory;
	private readonly engineOptionsFactory: EngineOptionsFactory;

	protected constructor(logger: Logger, display: Display, inputsResolver: InputsResolver, ruleFilterFactory: RuleFilterFactory,
							runOptionsFactory: RunOptionsFactory, engineOptionsFactory: EngineOptionsFactory) {
		this.logger = logger;
		this.display = display;
		this.inputsResolver = inputsResolver;
		this.ruleFilterFactory = ruleFilterFactory;
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
					throw new SfError(getMessage(BundleName.CommonRun, 'validations.projectdirCannotBeGlob'));
				} else if (!(await fh.exists(dir))) {
					throw new SfError(getMessage(BundleName.CommonRun, 'validations.projectdirMustExist'));
				} else if (!(await fh.stats(dir)).isDirectory()) {
					throw new SfError(getMessage(BundleName.CommonRun, 'validations.projectdirMustBeDir'));
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
				throw new SfError(getMessage(BundleName.CommonRun, 'validations.cannotWriteTableToFile', []));
			}
			// Otherwise, we want to be liberal with the user. If the chosen format doesn't match the outfile's extension,
			// just log a message saying so.
			if (chosenFormat !== inferredOutfileFormat) {
				this.display.displayInfo(getMessage(BundleName.CommonRun, 'validations.outfileFormatMismatch', [inputs.format as string, inferredOutfileFormat]));
			}
		}
	}

	async run(inputs: Inputs): Promise<AnyJson> {
		const filters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);
		const targetPaths: string[] = this.inputsResolver.resolveTargetPaths(inputs);
		const runOptions: RunOptions = this.runOptionsFactory.createRunOptions(inputs);
		const engineOptions: EngineOptions = this.engineOptionsFactory.createEngineOptions(inputs);

		// TODO: Inject RuleManager as a dependency to improve testability by removing coupling to runtime implementation
		const ruleManager: RuleManager = await Controller.createRuleManager();

		let output: RecombinedRuleResults = null;
		try {
			const results: Results = await ruleManager.runRulesMatchingCriteria(filters, targetPaths, runOptions, engineOptions);

			this.logger.trace(`Recombining results into requested format ${runOptions.format}`);
			const formattedOutput: FormattedOutput = await results.toFormattedOutput(runOptions.format, engineOptions.has(CUSTOM_CONFIG.VerboseViolations));
			output = {minSev: results.getMinSev(), results: formattedOutput, summaryMap: results.getSummaryMap()};

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

import {Flags, Ux} from '@salesforce/sf-plugins-core';
import {SfError} from '@salesforce/core';
import {AnyJson} from '@salesforce/ts-types';
import {ScannerCommand} from './ScannerCommand';
import {LooseObject, RecombinedRuleResults} from '../types';
import {RunOutputOptions, RunOutputProcessor} from './util/RunOutputProcessor';
import {Controller} from '../Controller';
import {OUTPUT_FORMAT, RuleManager, RunOptions} from './RuleManager';
import untildify = require('untildify');
import normalize = require('normalize-path');
import {RuleFilter} from "./RuleFilter";
import {RuleFilterFactory} from "./RuleFilterFactory";
import {RunOptionsFactory} from "./RunOptionsFactory";
import {Config} from "@oclif/core";
import {InputValidatorFactory} from "./InputValidatorFactory";
import {PathFactory} from "./PathFactory";
import {EngineOptionsFactory} from "./EngineOptionsFactory";
import {BUNDLE, getBundledMessage} from "../MessageCatalog";
import {Loggable} from "./Loggable";

// This code is used for internal errors.
export const INTERNAL_ERROR_CODE = 1;

export abstract class ScannerRunCommand extends ScannerCommand {
	/**
	 * There are flags that are common to all variants of the run command. We can define those flags
	 * here to avoid duplicate code.
	 */
	public static readonly flags = {
		verbose: Flags.boolean({
			summary: getBundledMessage(BUNDLE.COMMON, 'flags.verboseSummary')
		}),
		// BEGIN: Filter-related flags.
		category: Flags.custom<string[]>({
			char: 'c',
			summary: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.categorySummary'),
			description: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.categoryDescription'),
			delimiter: ',',
			multiple: true
		})(),
		// BEGIN: Flags related to results processing.
		format: Flags.custom<OUTPUT_FORMAT>({
			char: 'f',
			summary: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.formatSummary'),
			description: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.formatDescription'),
			options: Object.values(OUTPUT_FORMAT)
		})(),
		outfile: Flags.string({
			char: 'o',
			summary: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.outfileSummary'),
			description: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.outfileDescription')
		}),
		'severity-threshold': Flags.integer({
			char: 's',
			summary: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.sevthresholdSummary'),
			description: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.sevthresholdDescription'),
			exclusive: ['json'],
			min: 1,
			max: 3
		}),
		'normalize-severity': Flags.boolean({
			summary: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.normalizesevSummary'),
			description: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.normalizesevDescription')
		}),
		// END: Flags related to results processing.
		// BEGIN: Flags related to targeting.
		projectdir: Flags.custom<string[]>({
			char: 'p',
			summary: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.projectdirSummary'),
			description: getBundledMessage(BUNDLE.COMMON_RUN, 'flags.projectdirDescription'),
			parse: val => Promise.resolve(val.split(',').map(d => normalize(untildify(d))))
		})(),
		// END: Flags related to targeting.
	};

	private readonly ruleFilterFactory: RuleFilterFactory;
	private readonly runOptionsFactory: RunOptionsFactory;
	private readonly inputValidatorFactory: InputValidatorFactory;
	private readonly pathFactory: PathFactory;
	private readonly engineOptionsFactory: EngineOptionsFactory;

	protected constructor(argv: string[], config: Config,
						  inputValidatorFactory: InputValidatorFactory,
						  ruleFilterFactory: RuleFilterFactory,
						  runOptionsFactory: RunOptionsFactory,
						  pathFactory: PathFactory,
						  enginOptionsFactory: EngineOptionsFactory) {
		super(argv, config);
		this.ruleFilterFactory = ruleFilterFactory;
		this.runOptionsFactory = runOptionsFactory;
		this.inputValidatorFactory = inputValidatorFactory;
		this.pathFactory = pathFactory;
		this.engineOptionsFactory = enginOptionsFactory;
	}

	async runInternal(): Promise<AnyJson> {
		// TODO: Refactor ScannerCommand so that the parsedFlags can be passed into runInternal to effectively remove the state
		const inputs: LooseObject = this.parsedFlags;

		// Using this as the uxLogger
		const uxLogger: Loggable = this;
		await this.inputValidatorFactory.createInputValidator(uxLogger).validate(inputs);

		const filters: RuleFilter[] = this.ruleFilterFactory.createRuleFilters(inputs);
		const runOptions: RunOptions = this.runOptionsFactory.createRunOptions(inputs);
		const targetPaths: string[] = this.pathFactory.createTargetPaths(inputs);
		const engineOptions: Map<string, string> = this.engineOptionsFactory.createEngineOptions(inputs);

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
		return new RunOutputProcessor(outputOptions, new Ux({jsonEnabled: this.jsonEnabled()}))
			.processRunOutput(output);
	}
}

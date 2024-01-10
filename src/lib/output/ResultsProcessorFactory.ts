import {Display} from "../Display";
import {JsonReturnValueHolder} from "./JsonReturnValueHolder";
import {RunOutputOptions, RunResultsProcessor} from "./RunResultsProcessor";
import {CompositeResultsProcessor, ResultsProcessor} from "./ResultsProcessor";
import {ENV_VAR_NAMES} from "../../Constants";
import {inferFormatFromInternalOutfile, OutputFormat} from "./OutputFormat";
import {OutfileResultsProcessor} from "./OutfileResultsProcessor";

/**
 * Interface for creating a ResultsProcessor
 */
export interface ResultsProcessorFactory {
	createResultsProcessor(display: Display, runOutputOptions: RunOutputOptions,
							jsonReturnValueHolder: JsonReturnValueHolder): ResultsProcessor
}

/**
 * Runtime implementation of the ResultsProcessorFactory interface
 */
export class ResultsProcessorFactoryImpl implements ResultsProcessorFactory {
	public createResultsProcessor(display: Display, runOutputOptions: RunOutputOptions,
									jsonReturnValueHolder: JsonReturnValueHolder): ResultsProcessor {
		const resultsProcessors: ResultsProcessor[] = [new RunResultsProcessor(display, runOutputOptions, jsonReturnValueHolder)];
		this.addProcessorForInternalOutfileIfNeeded(resultsProcessors, runOutputOptions.verboseViolations);
		return new CompositeResultsProcessor(resultsProcessors);
	}

	private addProcessorForInternalOutfileIfNeeded(resultsProcessors: ResultsProcessor[], verboseViolations: boolean): void {
		const internalOutfile: string = process.env[ENV_VAR_NAMES.SCANNER_INTERNAL_OUTFILE];
		if (internalOutfile && internalOutfile.length > 0) {
			const internalOutputFormat: OutputFormat = inferFormatFromInternalOutfile(internalOutfile);
			resultsProcessors.push(new OutfileResultsProcessor(internalOutputFormat, internalOutfile, verboseViolations));
		}
	}
}

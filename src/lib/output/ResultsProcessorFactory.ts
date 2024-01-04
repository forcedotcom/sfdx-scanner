import {Display} from "../Display";
import {JsonReturnValueHolder} from "./JsonReturnValueHolder";
import {RunOutputOptions, RunResultsProcessor} from "./RunResultsProcessor";
import {CompositeResultsProcessor, ResultsProcessor} from "./ResultsProcessor";
import {ENV_VAR_NAMES} from "../../Constants";
import {inferFormatFromInternalOutfile, OutputFormat} from "./OutputFormat";
import {OutfileResultsProcessor} from "./OutfileResultsProcessor";

export interface ResultsProcessorFactory {
	createResultsProcessor(display: Display, runOutputOptions: RunOutputOptions,
							jsonReturnValueHolder: JsonReturnValueHolder): ResultsProcessor
}

export class ResultsProcessorFactoryImpl implements ResultsProcessorFactory {
	public createResultsProcessor(display: Display, runOutputOptions: RunOutputOptions,
									jsonReturnValueHolder: JsonReturnValueHolder): ResultsProcessor {

		const resultsProcessors: ResultsProcessor[] = [new RunResultsProcessor(display, runOutputOptions, jsonReturnValueHolder)];

		const internalOutfile: string = process.env[ENV_VAR_NAMES.SCANNER_INTERNAL_OUTFILE];
		if (internalOutfile && internalOutfile.length > 0) {
			const internalOutputFormat: OutputFormat = inferFormatFromInternalOutfile(internalOutfile);
			resultsProcessors.push(new OutfileResultsProcessor(internalOutputFormat, internalOutfile, runOutputOptions.verboseViolations));
		}

		return new CompositeResultsProcessor(resultsProcessors);
	}
}

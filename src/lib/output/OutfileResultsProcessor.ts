import {ResultsProcessor} from "./ResultsProcessor";
import {Results} from "./Results";
import {OutputFormat} from "./OutputFormat";
import {FormattedOutput} from "../../types";
import {FileHandler} from "../util/FileHandler";

/**
 * Processes results to produce an output file
 */
export class OutfileResultsProcessor implements ResultsProcessor {
	private readonly outputFormat: OutputFormat;
	private readonly outfile: string;
	private readonly verboseViolations: boolean;
	public constructor(format:OutputFormat, outfile: string, verboseViolations: boolean) {
		this.outputFormat = format;
		this.outfile = outfile;
		this.verboseViolations = verboseViolations;
	}

	public async processResults(results: Results): Promise<void> {
		const fileContents: FormattedOutput = await results.toFormattedOutput(this.outputFormat, this.verboseViolations);
		(new FileHandler()).writeFileSync(this.outfile, fileContents as string);
	}
}

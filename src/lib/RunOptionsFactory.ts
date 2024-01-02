import {Inputs} from "../types";
import {OUTPUT_FORMAT, RunOptions} from "./RuleManager";
import {SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../MessageCatalog";
import {INTERNAL_ERROR_CODE} from "../Constants";

/**
 * Service for processing inputs to create RunOptions
 */
export interface RunOptionsFactory {
	createRunOptions(inputs: Inputs): RunOptions;
}

export class RunOptionsFactoryImpl implements RunOptionsFactory {
	private readonly isRunDfa: boolean;
	private readonly sfVersion: string;

	public constructor(isRunDfa: boolean, sfVersion: string) {
		this.isRunDfa = isRunDfa;
		this.sfVersion = sfVersion;
	}

	public createRunOptions(inputs: Inputs): RunOptions {
		const runOptions: RunOptions = {
			format: determineOutputFormat(inputs.format as string, inputs.outfile as string, inputs.json as boolean),
			normalizeSeverity: (inputs['normalize-severity'] || inputs['severity-threshold']) as boolean,
			runDfa: this.isRunDfa,
			withPilot: inputs['with-pilot'] as boolean,
			sfVersion: this.sfVersion
		};
		return runOptions;
	}
}

function determineOutputFormat(format: string, outfile: string, json: boolean): OUTPUT_FORMAT {
	if (format) {
		return format as OUTPUT_FORMAT;
	} else if (outfile) {
		return inferFormatFromOutfile(outfile);
	} else if (json) {
		return OUTPUT_FORMAT.JSON;
	} else {
		return OUTPUT_FORMAT.TABLE;
	}
}

export function inferFormatFromOutfile(outfile: string): OUTPUT_FORMAT {
	const lastPeriod: number = outfile.lastIndexOf('.');
	if (lastPeriod < 1 || lastPeriod + 1 === outfile.length) {
		throw new SfError(getMessage(BundleName.CommonRun, 'validations.outfileMustBeValid'), null, null, INTERNAL_ERROR_CODE);
	}
	const fileExtension: string = outfile.slice(lastPeriod + 1).toLowerCase();
	switch (fileExtension) {
		case OUTPUT_FORMAT.CSV:
		case OUTPUT_FORMAT.HTML:
		case OUTPUT_FORMAT.JSON:
		case OUTPUT_FORMAT.SARIF:
		case OUTPUT_FORMAT.XML:
			return fileExtension;
		default:
			throw new SfError(getMessage(BundleName.CommonRun, 'validations.outfileMustBeSupportedType'), null, null, INTERNAL_ERROR_CODE);
	}
}

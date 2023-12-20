import {LooseObject} from "../types";
import {OUTPUT_FORMAT, RunOptions} from "./RuleManager";
import {Messages, SfError} from "@salesforce/core";
import {INTERNAL_ERROR_CODE} from "./ScannerRunCommand";

// TODO: Consider wrapping message loading inside of a class
const commonRunMessages: Messages<string> = Messages.loadMessages('@salesforce/sfdx-scanner', 'run-common');

export interface RunOptionsFactory {
	createRunOptions(inputs: LooseObject): RunOptions;
}

export class RunOptionsFactoryImpl implements RunOptionsFactory {
	private readonly isRunDfa: boolean;
	private readonly sfVersion: string;

	public constructor(isRunDfa: boolean, sfVersion: string) {
		this.isRunDfa = isRunDfa;
		this.sfVersion = sfVersion;
	}

	public createRunOptions(inputs: LooseObject): RunOptions {
		const runOptions: RunOptions = {
			format: determineOutputFormat(inputs.format, inputs.outfile, inputs.json),
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
		throw new SfError(commonRunMessages.getMessage('validations.outfileMustBeValid'), null, null, INTERNAL_ERROR_CODE);
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
			throw new SfError(commonRunMessages.getMessage('validations.outfileMustBeSupportedType'), null, null, INTERNAL_ERROR_CODE);
	}
}

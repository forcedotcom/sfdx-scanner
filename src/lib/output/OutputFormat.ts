import {SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import {ENV_VAR_NAMES, INTERNAL_ERROR_CODE} from "../../Constants";

export enum OutputFormat {
	CSV = 'csv',
	HTML = 'html',
	JSON = 'json',
	JUNIT = 'junit',
	SARIF = 'sarif',
	TABLE = 'table',
	XML = 'xml'
}

export function inferFormatFromOutfile(outfile: string): OutputFormat {
	return determineOutputFormat(outfile,
		getMessage(BundleName.CommonRun, 'validations.outfileMustBeValid'),
		getMessage(BundleName.CommonRun, 'validations.outfileMustBeSupportedType'))
}

export function inferFormatFromInternalOutfile(outfile: string): OutputFormat {
	return determineOutputFormat(outfile,
		getMessage(BundleName.CommonRun, 'internal.outfileMustBeValid', [ENV_VAR_NAMES.SCANNER_INTERNAL_OUTFILE]),
		getMessage(BundleName.CommonRun, 'internal.outfileMustBeSupportedType', [ENV_VAR_NAMES.SCANNER_INTERNAL_OUTFILE]));
}

function determineOutputFormat(outfile: string, invalidFileMsg: string, invalidExtensionMsg: string): OutputFormat {
	const lastPeriod: number = outfile.lastIndexOf('.');
	if (lastPeriod < 1 || lastPeriod + 1 === outfile.length) {
		throw new SfError(invalidFileMsg, null, null, INTERNAL_ERROR_CODE);
	}
	const fileExtension: string = outfile.slice(lastPeriod + 1).toLowerCase();
	switch (fileExtension) {
		case OutputFormat.CSV.toString():
		case OutputFormat.HTML.toString():
		case OutputFormat.JSON.toString():
		case OutputFormat.SARIF.toString():
		case OutputFormat.XML.toString():
			return fileExtension as OutputFormat;
		default:
			throw new SfError(invalidExtensionMsg, null, null, INTERNAL_ERROR_CODE);
	}
}

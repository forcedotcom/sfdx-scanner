import {SfError} from "@salesforce/core";
import {BundleName, getMessage} from "../../MessageCatalog";
import {INTERNAL_ERROR_CODE} from "../../Constants";

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
	// TODO: Since the this is being used now for both the --outfile and the internal outfile, we should move where
	//  validation takes place for --outfile and make these messages more generic.

	const lastPeriod: number = outfile.lastIndexOf('.');
	if (lastPeriod < 1 || lastPeriod + 1 === outfile.length) {
		throw new SfError(getMessage(BundleName.CommonRun, 'validations.outfileMustBeValid'), null, null, INTERNAL_ERROR_CODE);
	}
	const fileExtension: string = outfile.slice(lastPeriod + 1).toLowerCase();
	switch (fileExtension) {
		case OutputFormat.CSV:
		case OutputFormat.HTML:
		case OutputFormat.JSON:
		case OutputFormat.SARIF:
		case OutputFormat.XML:
			return fileExtension;
		default:
			throw new SfError(getMessage(BundleName.CommonRun, 'validations.outfileMustBeSupportedType'), null, null, INTERNAL_ERROR_CODE);
	}
}

import {SfError} from "@salesforce/core";
import {INTERNAL_ERROR_CODE} from "../../Constants";
import fs = require('fs');

export function writeToFile(file: string, fileContents: string): void {
	try {
		fs.writeFileSync(file, fileContents);
	} catch (e) {
		// Rethrow any errors as SfError.
		const message: string = e instanceof Error ? e.message : e as string;
		throw new SfError(message, null, null, INTERNAL_ERROR_CODE);
	}
}

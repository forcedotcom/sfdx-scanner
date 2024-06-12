import * as fs from 'node:fs';
import {OutputFormat, RunResults} from '@salesforce/code-analyzer-core';
import {BundleName, getMessage} from '../messages';

export interface OutputFileWriter {
	writeToFiles(results: RunResults): void;
}

export class OutputFileWriterImpl implements OutputFileWriter {
	private outputFilesToFileFormat: Map<string, OutputFormat> = new Map();

	public constructor(files: string[]) {
		for (const file of files) {
			if (file.endsWith(`.csv`)) {
				this.outputFilesToFileFormat.set(file, OutputFormat.CSV);
			} else if (file.endsWith('.html') || file.endsWith('.htm')) {
				throw new Error('TODO: Support HTML-type output');
			} else if (file.endsWith('.sarif') || file.endsWith('.sarif.json')) {
				throw new Error('TODO: Support SARIF-type output');
			// Check for `.json` AFTER checking for `.sarif.json`!
			} else if (file.endsWith('.json')) {
				this.outputFilesToFileFormat.set(file, OutputFormat.JSON);
			} else if (file.endsWith('.junit') || file.endsWith('.junit.xml')) {
				throw new Error('TODO: Support JUNIT-type output');
			// Check for `.xml` AFTER checking for `.junit.xml`!
			} else if (file.endsWith('.xml')) {
				this.outputFilesToFileFormat.set(file, OutputFormat.XML);
			} else {
				throw new Error(getMessage(BundleName.OutputFileWriter, 'error.unrecognized-file-format', [file]));
			}
		}
	}

	public writeToFiles(results: RunResults): void {
		for (const [file, format] of this.outputFilesToFileFormat.entries()) {
			fs.writeFileSync(file, results.toFormattedOutput(format));
		}
	}
}

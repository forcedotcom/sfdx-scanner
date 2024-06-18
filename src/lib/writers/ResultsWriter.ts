import * as fs from 'node:fs';
import path from 'node:path';
import {OutputFormat, RunResults} from '@salesforce/code-analyzer-core';
import {BundleName, getMessage} from '../messages';

export interface ResultsWriter {
	write(results: RunResults): void;
}

export class CompositeResultsFileWriter implements ResultsWriter {
	private readonly writers: ResultsFileWriter[] = [];

	private constructor(files: string[]) {
		for (const file of files) {
			this.writers.push(new ResultsFileWriter(file));
		}
	}

	public write(results: RunResults): void {
		this.writers.forEach(w => w.write(results));
	}

	public static getWriter(files: string[]): CompositeResultsFileWriter {
		return new CompositeResultsFileWriter(files);
	}
}

export class ResultsFileWriter implements ResultsWriter {
	private readonly file: string;
	private readonly format: OutputFormat;

	public constructor(file: string) {
		this.file = file;

		if (path.extname(file).toLowerCase() === '.csv') {
			this.format = OutputFormat.CSV;
		} else if (['.html', '.htm'].includes(path.extname(file).toLowerCase())) {
			throw new Error('TODO: Support HTML-type output');
		} else if (file.toLowerCase().endsWith('.sarif') || file.toLowerCase().endsWith('.sarif.json')) {
			throw new Error('TODO: Support SARIF-type output');
		// Check for `.json` AFTER checking for `.sarif.json`!
		} else if (path.extname(file).toLowerCase() === '.json') {
			this.format = OutputFormat.JSON;
		} else if (file.toLowerCase().endsWith('.junit') || file.toLowerCase().endsWith('.junit.xml')) {
			throw new Error('TODO: Support JUNIT-type output');
		// Check for `.xml` AFTER checking for `.junit.xml`!
		} else if (path.extname(file).toLowerCase() === '.xml') {
			this.format = OutputFormat.XML;
		} else {
			throw new Error(getMessage(BundleName.OutputFileWriter, 'error.unrecognized-file-format', [file]));
		}
	}

	public write(results: RunResults): void {
		fs.writeFileSync(this.file, results.toFormattedOutput(this.format));
	}
}

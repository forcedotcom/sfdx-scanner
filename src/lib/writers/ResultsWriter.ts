import * as fs from 'node:fs';
import path from 'node:path';
import {OutputFormat, RunResults} from '@salesforce/code-analyzer-core';
import {BundleName, getMessage} from '../messages';

export interface ResultsWriter {
	write(results: RunResults): void;
}

export class CompositeResultsWriter implements ResultsWriter {
	private readonly writers: ResultsWriter[] = [];

	private constructor(writers: ResultsWriter[]) {
		this.writers = writers;
	}

	public write(results: RunResults): void {
		this.writers.forEach(w => w.write(results));
	}

	public static fromFiles(files: string[]): CompositeResultsWriter {
		return new CompositeResultsWriter(files.map(f => new ResultsFileWriter(f)));
	}
}

export class ResultsFileWriter implements ResultsWriter {
	private readonly file: string;
	private readonly format: OutputFormat;

	public constructor(file: string) {
		this.file = file;
		const ext = path.extname(file).toLowerCase();
		if (ext === '.csv') {
			this.format = OutputFormat.CSV;
		} else if (['.html', '.htm'].includes(ext)) {
			this.format = OutputFormat.HTML;
		} else if (ext === '.sarif' || file.toLowerCase().endsWith('.sarif.json')) {
			throw new Error('TODO: Support SARIF-type output');
		// Check for `.json` AFTER checking for `.sarif.json`!
		} else if (ext === '.json') {
			this.format = OutputFormat.JSON;
		} else if (ext === '.junit' || file.toLowerCase().endsWith('.junit.xml')) {
			throw new Error('TODO: Support JUNIT-type output');
		// Check for `.xml` AFTER checking for `.junit.xml`!
		} else if (ext === '.xml') {
			this.format = OutputFormat.XML;
		} else {
			throw new Error(getMessage(BundleName.ResultsWriter, 'error.unrecognized-file-format', [file]));
		}
	}

	public write(results: RunResults): void {
		fs.writeFileSync(this.file, results.toFormattedOutput(this.format));
	}
}
